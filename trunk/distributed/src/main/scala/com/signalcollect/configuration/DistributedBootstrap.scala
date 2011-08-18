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

package com.signalcollect.configuration

import com.signalcollect.interfaces._
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.implementations.worker._
import com.signalcollect.implementations.manager._
import com.signalcollect.implementations.coordinator._
import com.signalcollect.implementations.logging._
import com.signalcollect.implementations.messaging._
import com.signalcollect.util._
import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.actor.PoisonPill
import com.signalcollect.Graph
import com.signalcollect.factory.worker.AkkaRemoteReference

/**
 * The bootstrap sequence for initializing the distributed infrastructure
 *
 * akka.conf usage:
 *
 * -Dakka.config=akka.conf
 *
 */
class DistributedBootstrap(var config: DefaultDistributedConfiguration) extends Bootstrap with BootstrapHelper {

  // reference to the coordinator forwarder actor
  var coordinatorForwarder: ActorRef = _

  // reference to the leader manager actor
  var leaderManager: ActorRef = _

  // reference to the boot manager
  var bootManager: ActorRef = _

  /**
   * Starts essential services for machine discovery and determination of machine type via simple leader election
   */
  def start: MachineType = {

    // start Akka remote server
    remote.start(localIp, Constants.REMOTE_SERVER_PORT)

    // register leader manager (everyone is a leader in the beginning
    remote.register(Constants.LEADER_NAME, actorOf(new LeaderManager(config.numberOfMachines)))

    // register bootstrap manager
    remote.register(Constants.BOOT_NAME, actorOf(new BootstrapManager(config.numberOfMachines)))

    // get his reference
    bootManager = remote.actorFor(Constants.BOOT_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    // start hazelcast machine finding
    val machinesAddress = startHazelcastGetAddresses(config.numberOfMachines)

    // tell the bootmanager what machine ips came online
    bootManager ! MachinesAddress(machinesAddress)

    // leader ip from boot manager
    var leaderIp = getLeaderIpFromBootManager(bootManager)

    // set ip of leader in config
    config.leaderAddress = leaderIp

    // put all the ips on the machinesAddress in config
    config.machinesAddress = machinesAddress

    // am I the leader?
    if (localIp equals leaderIp)
      LeaderType
    // I am zombie
    else
      ZombieType

  }

  /**
   * Start the necessary services for zombies to communicate with leader
   */
  def deployZombie {

    // kill the local leader manager
    leaderManager = remote.actorFor(Constants.LEADER_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    leaderManager ! PoisonPill

    // start zombie manager
    remote.register(Constants.ZOMBIE_NAME, actorOf(new ZombieManager(config.leaderAddress)))

    val zombieManager = remote.actorFor(Constants.ZOMBIE_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    // after successful zombie start, the zombie can tell the leader it is alive
    zombieManager ! SendAlive

    // kill bootmanager
    bootManager ! PoisonPill

    // terminate hazelcast
    shutdownHazelcast

  }

  /**
   * Start the necessary services for zombies to communicate with coordinator staying at the leader (worker infrastructure)
   * Also blocks until all workers have finished being instantiated
   */
  def deployLeader {

    leaderManager = remote.actorFor(Constants.LEADER_NAME, localIp, Constants.REMOTE_SERVER_PORT)

    checkAlive(leaderManager)

    // start coordinator forwarder which will receive coordinator messages
    remote.register(Constants.COORDINATOR_NAME, actorOf(new AkkaCoordinatorForwarder(config.numberOfWorkers)))

    // get its hook
    coordinatorForwarder = remote.actorFor(Constants.COORDINATOR_NAME, config.leaderAddress, Constants.REMOTE_SERVER_PORT)

    // create my local workers, i.e, create workers staying at the leader
    createLocalWorkers(coordinatorForwarder, config)

  }

  /**
   * Executed by the leader, it gets the references of all the workers, distributed and local ones 
   */
  def createWorkers(workerApi: WorkerApi) {

    // create a hook for all worker instances (whether local or remote), the leader will distribute them when executing workerApi.initialize @see WorkerApi.initialize
    for (workerId <- 0 until config.numberOfWorkers) {

      config.workerConfiguration.workerFactory match {
        case AkkaRemoteReference =>

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
  def bootOption: Option[Graph] = {

    // from leader election phase, get machine type
    val machineType = start

    // optional compute graph
    var optionalCg: Option[Graph] = None

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

        // wait until all zombies come alive
        waitZombie(leaderManager, CheckAllAlive)

        // terminate hazelcast
        shutdownHazelcast

        // kill bootmanager
        bootManager ! PoisonPill

        // send configuration parameters to leader after it has been properly set by provisioning
        leaderManager ! ConfigPackage(config)

        // wait until all zombies have instantiated their remote workers
        waitZombie(leaderManager, CheckAllReady)

        // create optional logger
        var logger = if (config.customLogger.isDefined)
          config.customLogger.get
        else
          createLogger

        val workerApi = new AkkaWorkerApi(config, logger)

        createWorkers(workerApi)

        // set coordinator at the forwarder (now that it is correctly available)
        coordinatorForwarder ! CoordinatorReference(workerApi)

        // distribute infrastructure information
        workerApi.initialize

        val coordinator = new Coordinator(workerApi, config)

        // create the compute graph
        computeGraph = createGraph(workerApi, coordinator)

        // return it for execution
        optionalCg = Some(computeGraph)
    }

    optionalCg

  }

}
