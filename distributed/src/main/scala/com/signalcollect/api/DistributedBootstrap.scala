/*
 *  @author Francisco de Freitas
 *  
 *  Copyright 2011 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package com.signalcollect.api

import com.signalcollect.api.factory._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.interfaces.MessageRecipient
import com.signalcollect.interfaces.ComputeGraph
import com.signalcollect.interfaces.LogMessage
import com.signalcollect.implementations.worker._
import com.signalcollect.implementations.manager._
import com.signalcollect.implementations.coordinator._
import com.signalcollect.implementations.logging._
import com.signalcollect.implementations.messaging._
import com.signalcollect.util._

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

import com.hazelcast.core._

import scala.util.Random
import scala.collection.JavaConversions._

/**
 * The bootstrap sequence for initializing the distributed infrastructure
 *
 * akka.conf usage:
 *
 * -Dakka.config=akka.conf
 *
 */
class DistributedBootstrap(var config: DefaultDistributedConfiguration) extends Bootstrap with RemoteSendUtils with BootstrapHelper {

  // the machine's local IP
  val localIp = java.net.InetAddress.getLocalHost.getHostAddress

  // reference to the coordinator forwarder
  var coordinatorForwarder: ActorRef = _

  // reference to the leader manager actor
  var leaderManager: ActorRef = _

  /**
   * Starts essential services for machine discovery and determination of machine type via leader election
   */
  def start: MachineType = {

    //println("Starting machine services...")

    // start Akka remote server
    remote.start(localIp, Constants.REMOTE_SERVER_PORT)

    /** Everyone has a leader service in the beginning */
    remote.register("leader-service", actorOf(new LeaderManager(config.numberOfNodes)))

    //println("<<<<<< Hazelcast >>>>>>")

    var leaderIp = ""

    // if only one node, avoid using hazelcast
    if (config.numberOfNodes != 1) {

      // hazelcast config
      val hcConfigXml = new com.hazelcast.config.FileSystemXmlConfig(getHazelcastConfigFile) //-Dhazelcast.logging.type=none

      // hazelcast cluster
      val cluster = Hazelcast.init(hcConfigXml).getCluster

      // map of members joining the hazelcast cluster
      val distributedMap: com.hazelcast.core.IMap[String, Long] = Hazelcast.getMap("members")

      // map put ip + random generated long
      //distributedMap.put(localIp, Random.nextLong.abs)
      distributedMap.put(localIp, -1l)
      //distributedMap.put(localIp, Long.MaxValue)

      println("... Waiting Hazelcast all joined ...") // TODO: ADD TIMEOUT
      // wait until all nodes have joined
      while (config.numberOfNodes != distributedMap.keySet().size) {
        Thread.sleep(500)
      }

      // get ip with smallest long
      val leaderId = distributedMap.foldLeft(Long.MaxValue)((min, kv) => Math.min(min, kv._2))

      distributedMap.foreach(x => if (x._2 == leaderId) leaderIp = x._1)

      // set ip of leader in config
      config.leaderAddress = leaderIp

      // put the other ips on the nodesAddress in config
      config.nodesAddress = distributedMap.keySet().toList

      // terminate hazelcast
      Hazelcast.shutdownAll
    } else {
      leaderIp = localIp
      config.leaderAddress = leaderIp
      config.nodesAddress = List(localIp)
    }

    // if local ip == ip with smallest long, then I am the leader
    if (localIp equals leaderIp)
      LeaderType
    // else I am zombie
    else
      ZombieType

  }

  /**
   * Start the necessary services for zombies to communicate with leader
   */
  def deployZombie {

    println("ZOMBIE... Braaaaaiinnnsss")

    // start zombie manager
    remote.register(Constants.ZOMBIE_MANAGER_SERVICE_NAME, actorOf(new ZombieManager(config.leaderAddress)))

    val zombieManager = remote.actorFor(Constants.ZOMBIE_MANAGER_SERVICE_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    zombieManager ! SendAlive

  }

  /**
   * Start the necessary services for zombies to communicate with coordinator staying at the leader (worker infrastructure)
   * Also blocks until all workers have finished being instantiated
   */
  def deployLeader {

    println("I'm the leader")

    leaderManager = remote.actorFor(Constants.LEADER_MANAGER_SERVICE_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    checkAlive(leaderManager)

    // start coordinator forwarder which will receive coordinator messages
    remote.register(Constants.COORDINATOR_SERVICE_NAME, actorOf(new AkkaCoordinatorForwarder(config.numberOfWorkers)))

    // get its hook
    coordinatorForwarder = remote.actorFor(Constants.COORDINATOR_SERVICE_NAME, config.leaderAddress, Constants.REMOTE_SERVER_PORT)

    // create my local workers, i.e, create workers staying at the leader
    createLocalWorkers(coordinatorForwarder, config)

  }

  protected def createLogger: MessageRecipient[LogMessage] = new DefaultLogger

  def createWorkers(workerApi: WorkerApi) {

    // create a hook for all worker instances (whether local or remote), the leader will distribute them when executing workerApi.initialize @see WorkerApi.initialize
    for (workerId <- 0 until config.numberOfWorkers) {

      config.workerConfiguration.workerFactory match {
        case worker.AkkaRemoteReference =>

          // workerConfiguration
          val workerConfig = DefaultRemoteWorkerReferenceConfiguration(ipAddress = config.workerConfigurations.get(workerId).get.ipAddress, serviceName = config.workerConfigurations.get(workerId).get.serviceName)

          // get remote hook
          val worker = workerApi.createWorker(workerId, workerConfig).asInstanceOf[ActorRef]

          checkAlive(worker)

        case _ => throw new Exception("Only Akka remote references supported by this DistributedAkkaBootstrap")
      }
    }

  }

  /**
   * Get per worker the Ip address of the machine where it will be instantiated + the service name of the worker
   * Add to that the configuration necessary for remote initialization
   *
   * @return optional compute graph. The compute graph is only used by the leader
   */
  def bootOption: Option[ComputeGraph] = {

    // from leader election phase, get node type
    val nodeType = start

    // optional compute graph
    var optionalCg: Option[ComputeGraph] = None

    // check for node type
    nodeType match {

      /** ZOMBIE */
      case ZombieType =>

        deployZombie // will not return a compute graph

      /** LEADER */
      case LeaderType =>

        // create provisioning
        val provisioning = config.provisionFactory.createInstance(config).workersPerNodeNames

        // deploy network services and remote infrastructure
        deployLeader

        println("Wait ALL ALIVE")
        waitZombie(leaderManager, CheckAllAlive)

        // send configuration parameters to leader after it has been properly set by provisioning
        leaderManager ! Config(config)

        println("Wait ALL READY")
        waitZombie(leaderManager, CheckAllReady)

        // after everyone has been setup we can shutdown the leader. this triggers zombie managers shutdown
        leaderManager ! Shutdown

        // create optional logger
        var logger = createLogger

        val workerApi = new AkkaWorkerApi(config, logger)

        createWorkers(workerApi)

        // set coordinator at the forwarder (now that it is correctly available)
        coordinatorForwarder ! CoordinatorReference(workerApi)

        workerApi.initialize

        val coordinator = new Coordinator(workerApi, config)

        computeGraph = createComputeGraph(workerApi, coordinator)

        optionalCg = Some(computeGraph)
    }

    optionalCg

  }

}
