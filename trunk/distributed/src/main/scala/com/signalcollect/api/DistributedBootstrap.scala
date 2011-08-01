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
import com.signalcollect.interfaces._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.implementations.worker._
import com.signalcollect.implementations.manager._
import com.signalcollect.implementations.coordinator._
import com.signalcollect.implementations.logging._
import com.signalcollect.implementations.messaging._
import com.signalcollect.util._

import akka.actor.{ ActorRegistry, Actor }
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.remoteinterface._

import com.hazelcast.core._

import scala.util.Random
import scala.collection.JavaConversions._

sealed trait NodeType
case class ZombieType extends NodeType
case class LeaderType extends NodeType

/**
 * The bootstrap sequence for initializing the distributed infrastructure
 *
 * akka.conf usage:
 *
 * -Dakka.config=lib/akka.conf
 *
 */
class DistributedBootstrap(var config: DefaultDistributedConfiguration) extends Bootstrap {

  val localIp = java.net.InetAddress.getLocalHost.getHostAddress

  var localCoordinatorForwarder: ActorRef = _

  def start: NodeType = {

    println("Starting node services...")

    // start Akka remote server
    remote.start(localIp, Constants.REMOTE_SERVER_PORT)

    println("<<<<<< Hazelcast >>>>>>")

    // hazelcast config
    val hcConfigXml = new com.hazelcast.config.FileSystemXmlConfig(new java.io.File("lib/hazelcast.xml"))

    // hazelcast cluster
    val cluster = Hazelcast.init(hcConfigXml).getCluster

    val distributedMap: com.hazelcast.core.IMap[String, Long] = Hazelcast.getMap("members")

    // map put ip + random generated long
    distributedMap.put(localIp, Random.nextLong.abs)

    println("... Waiting untill all have joined ...") // TODO: ADD TIMEOUT
    // wait until all nodes have joined
    while (config.numberOfNodes != distributedMap.keySet().size()) {
      Thread.sleep(500)
    }

    // convert map to scala map
    val keys = distributedMap.toMap

    // get ip with smallest long
    val leaderId = distributedMap.foldLeft(Long.MaxValue)((min, kv) => Math.min(min, kv._2))

    var leaderIp = ""

    distributedMap.foreach(x => if (x._2 == leaderId) leaderIp = x._1)

    println("Leader IP = " + leaderIp)
    println("LOCAL IP = " + localIp)

    // put this ip on the coordinator address in config
    config.coordinatorAddress = leaderIp

    // put the other ips on the nodesAddress in config
    config.nodesAddress = distributedMap.keySet().toList
    
    Hazelcast.shutdownAll()

    /** Everyone is a leader in the beginning */
    remote.register(Constants.LEADER_MANAGER_SERVICE_NAME, actorOf(new LeaderManager(config)))

    // if local ip == ip with smallest long, then I am the leader
    if (localIp equals leaderIp) {
      println("I'm the leader")
      LeaderType()
    } // else I am zombie, shutdown my leader manager
    else {
      println("ZOMBIE... Braaaaaiinnnsss")
      ZombieType()
    }

  }

  /**
   * Start the necessary services for zombies to communicate with leader
   */
  def deployZombie {

    println("Deploying zombie")

    // start zombie manager
    remote.register(Constants.ZOMBIE_MANAGER_SERVICE_NAME, actorOf(new ZombieManager(config.coordinatorAddress)))

  }

  /**
   * Start the necessary services for zombies to communicate with coordinator staying at the leader (worker infrastructure)
   * Also blocks until all workers have finished being instantiated
   */
  def deployLeader {

    println("Deploying leader")

    // start coordinator forwarder which will receive coordinator messages
    remote.register(Constants.COORDINATOR_SERVICE_NAME, actorOf[AkkaCoordinatorForwarder])

    // create my local workers, i.e, create workers staying at the leader
    createLocalWorkers

    println("LEADER WAITING!!!!!")

    val leaderManager = remote.actorFor(Constants.LEADER_MANAGER_SERVICE_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    var allReady = false

    val timeout = 500l

    // blocking operation that asks the manager if everyone is ready, wait until everyone is
    println("... Waiting untill all zombies are ready ...") // TODO: ADD TIMEOUT
    while (!allReady) {
      Thread.sleep(100)

      val result: Option[Any] = leaderManager !! (CheckAllReady, timeout)

      result match {
        case Some(reply) => allReady = reply.asInstanceOf[Boolean] // handle reply
        case None        => sys.error("no reply within " + timeout + " ms")
      }

    }
    
    // after everyone has been setup we can shutdown the leader. this triggers zombie managers shutdown
    leaderManager ! Shutdown

  }

  protected def createLogger: MessageRecipient[LogMessage] = new DefaultLogger

  /**
   * Real worker instantiation / initialization at the leader node
   *
   */
  protected def createLocalWorkers {
    println("<<<<<< Local Workers >>>>>>")

    localCoordinatorForwarder = remote.actorFor(Constants.COORDINATOR_SERVICE_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

    // get only those workers that should be instantiated at this node 
    val workers = config.workerConfigurations.filter(x => x._2.ipAddress.equals(localIp))

    // start the workers
    for (idConfig <- workers) {

      val workerId = idConfig._1

      val workerConfig = idConfig._2

      val workerFactory = workerConfig.workerFactory /*worker.AkkaRemoteWorker*/

      /*// debug
      if (!workerConfig.workerFactory.equals(worker.AkkaRemoteWorker))
        sys.error("ooops, remote worker factory should be used. check bootstrap/configuration setup")*/

      // create the worker with the retrieved configuration (ip,port), coordinator reference, and mapper
      workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, localCoordinatorForwarder, mapper)

      val worker = remote.actorFor(workerConfig.asInstanceOf[RemoteWorkerConfiguration].serviceName, workerConfig.asInstanceOf[RemoteWorkerConfiguration].ipAddress, Constants.REMOTE_SERVER_PORT)

      val to = 500L

      val result: Option[Any] = worker !! ("Hello", to)

      result match {
        case Some(reply) => println(reply) // handle reply
        case None        => sys.error("no reply within " + to + " ms")
      }

    } // end for each worker

  }

  def createWorkers(workerApi: WorkerApi) {

  }

  /**
   * Leader "creation" of all workers
   */
  def createWorkers(workerApi: WorkerApiWithNewProxy) {

    println("*******************")
    println("***** WORKERS *****")
    println("*******************")

    // create a hook for all worker instances (whether local or remote), the leader will distribute them when executing workerApi.initialize @see WorkerApi.initialize
    for (workerId <- 0 until config.numberOfWorkers) {

      config.workerConfiguration.workerFactory match {
        case worker.AkkaRemoteReference =>

          // workerConfiguration
          val workerConfig = DefaultRemoteWorkerReferenceConfiguration(ipAddress = config.workerConfigurations.get(workerId).get.ipAddress, serviceName = config.workerConfigurations.get(workerId).get.serviceName)

          // get remote hook
          val worker = workerApi.createWorker(workerId, workerConfig).asInstanceOf[ActorRef]

          val to = 500L

          val result: Option[Any] = worker !! ("Hello", to)

          result match {
            case Some(reply) => println(reply) // handle reply
            case None        => sys.error("no reply within " + to + " ms")
          }

        case _ => throw new Exception("Only Akka remote references supported by this DistributedAkkaBootstrap")
      }
    }

  }

  def createComputeGraph(workerApi: WorkerApi, coordinator: Coordinator): DefaultComputeGraph = {
    new DefaultComputeGraph(config, workerApi, coordinator)
  }

  def shutdown {

    // signal managers
    println("shutdown")
  }

  /**
   * Get per worker the Ip address of the machine where it will be instantiated + the service name of the worker
   * Add to that the configuration necessary for remote initialization
   *
   * @return optional compute graph. The compute graph is only used by the leader
   */
  override def boot: Option[ComputeGraph] = {

    // from leader election phase, get node type
    val nodeType = start

    // optional compute graph
    var optionalCg: Option[ComputeGraph] = None

    // check for node type
    nodeType match {

      /** ZOMBIE */
      case ZombieType() =>

        deployZombie // will not return a compute graph

      /** LEADER */
      case LeaderType() =>

        println("Provisioning start...")

        // create provisioning
        val provisioning = config.provisionFactory.createInstance(config).workersPerNodeNames

        // deploy network services and remote infrastructure
        deployLeader

        // continue with coordinator normal bootstrap
        // create optional logger
        var logger = createLogger

        val workerApi = new WorkerApiWithNewProxy(config, logger)

        createWorkers(workerApi)

        localCoordinatorForwarder ! CoordinatorReference(workerApi)

        workerApi.initialize

        val coordinator = new Coordinator(workerApi, config)

        computeGraph = createComputeGraph(workerApi, coordinator)

        optionalCg = Some(computeGraph)
    }

    optionalCg
    
    

  }

}
