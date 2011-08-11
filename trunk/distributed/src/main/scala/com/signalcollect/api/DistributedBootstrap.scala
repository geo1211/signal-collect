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
import scala.collection.JavaConversions._
import com.hazelcast.core._
import scala.util.Random
import akka.actor.PoisonPill

/**
 * The bootstrap sequence for initializing the distributed infrastructure
 *
 * akka.conf usage:
 *
 * -Dakka.config=akka.conf
 *
 */
class DistributedBootstrap(var config: DefaultDistributedConfiguration) extends Bootstrap with BootstrapHelper {

  // reference to the coordinator forwarder
  var coordinatorForwarder: ActorRef = _

  // reference to the leader manager actor
  var leaderManager: ActorRef = _

  /**
   * Starts essential services for machine discovery and determination of machine type via leader election
   */
  def start: MachineType = {

    // start Akka remote server
    remote.start(localIp, Constants.REMOTE_SERVER_PORT)
    
    remote.register(Constants.LEADER_MANAGER_SERVICE_NAME, actorOf(new LeaderManager(config.numberOfMachines)))

    var hazelcastRetry = false

    while (!hazelcastRetry) {

      hazelcastRetry = false

      // hazelcast config
      val hcConfigXml = new com.hazelcast.config.FileSystemXmlConfig(getHazelcastConfigFile) //-Dhazelcast.logging.type=none

      // hazelcast cluster
      val cluster = Hazelcast.init(hcConfigXml).getCluster

      var retries = 0

      var retriesDone = false

      var lastSize = cluster.getMembers().size()

      while (cluster.getMembers().size() != config.numberOfMachines || retriesDone) {
        Thread.sleep(500)

        retries = retries + 1

        if (retries == 25) { // after 12,5 seconds
          retriesDone = true
          retries = 0
        }

        if (lastSize != cluster.getMembers().size())
          retries = 0

      }

      if (retriesDone) {
        Hazelcast.shutdownAll
      } else
        hazelcastRetry = true

    }

    // map of ips + random number
    val distributedMap: com.hazelcast.core.IMap[String, Long] = Hazelcast.getMap("members")

    distributedMap.put(localIp, Random.nextLong.abs)
    //distributedMap.put(localIp, -1l)
    //distributedMap.put(localIp, Long.MaxValue)

    // wait until all machines have joined
    println("Waiting HAZELCAST")
    while (config.numberOfMachines != distributedMap.keySet().size) {
      Thread.sleep(500)
    }
    println("Hazelcast Done!")

    var leaderIp = ""

    // get ip with smallest long
    val leaderId = distributedMap.foldLeft(Long.MaxValue)((min, kv) => Math.min(min, kv._2))
    distributedMap.foreach(x => if (x._2 == leaderId) leaderIp = x._1)

    // set ip of leader in config
    config.leaderAddress = leaderIp

    // put the other ips on the machinesAddress in config
    config.machinesAddress = distributedMap.keySet().toList

    // shutdown
    Hazelcast.shutdown

    // terminate hazelcast
    Hazelcast.shutdownAll
    /*} else {
      leaderIp = localIp
      config.leaderAddress = leaderIp
      config.machinesAddress = List(localIp)
    }*/

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
    
    leaderManager = remote.actorFor(Constants.LEADER_MANAGER_SERVICE_NAME, localIp, Constants.REMOTE_SERVER_PORT)
    
    leaderManager ! PoisonPill

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

    // from leader election phase, get machine type
    val machineType = start

    // optional compute graph
    var optionalCg: Option[ComputeGraph] = None

    // check for machine type
    machineType match {

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
