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

import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import com.signalcollect.implementations.manager._
import com.signalcollect.implementations.coordinator._
import com.signalcollect.implementations.logging._
import com.signalcollect.implementations.messaging._
import com.signalcollect.util._

import akka.actor.Actor
import akka.actor.Actor._
import akka.remoteinterface._

import java.net.InetAddress

sealed trait NodeType
case class ZombieType extends NodeType
case class LeaderType extends NodeType

/**
 * The bootstrap sequence for initializing the distributed infrastructure
 */
class DistributedBootstrap(val config: DistributedConfiguration) extends Bootstrap {
  
  val localIp = InetAddress.getLocalHost.getHostAddress

  def start: NodeType = {

    /** Everyone is a leader in the beginning */
    Actor.remote.start(localIp, Constants.MANAGER_SERVICE_PORT)
    Actor.remote.register(Constants.LEADER_MANAGER_SERVICE_NAME, actorOf[LeaderManager])

    // hazelcast cluster

    // map put ip + random generated long

    // wait map keyset == number of nodes

    // convert map to scala map

    // get ip with smallest long

    // put this ip on the coordinator address in config
    // put the other ips on the nodesAddress in config

    // if local ip == ip with smallest long, then I am the leader

    // else I am zombie, shutdown my leader manager

    ZombieType()

  }

  // TODO: Change this
  override def boot: ComputeGraph = null
  
  /**
   * Get per worker the Ip address of the machine where it will be instantiated + the port where it will listen
   * Add to that the configuration necessary for remote initialization
   */
  def boota: Option[ComputeGraph] = {

    // from leader election phase, get node type
    val nodeType = start
    
    var optionalCg: Option[ComputeGraph] = None

    // check for node type
    nodeType match {

      case ZombieType() =>
        
        deployZombie
        

      case LeaderType() =>
        // the leader needs to have access to the spreadsheet api + the code in the run of jobexecutor

        // create provisioning
        val provisioning = config.provisionFactory.createInstance(config)

        var workerIdCounter = 0

        // create worker configuration for each worker
        for (ipPorts <- provisioning.workerPorts) {

          val ip = ipPorts._1
          val ports = ipPorts._2

          // for each worker
          for (port <- ports) {

            // create specific configuration for the worker staying at the zombie
            val remoteWorkerConfiguration = DefaultRemoteWorkerConfiguration(ipAddress = ip, port = port)

            // add the worker configuration to the list
            config.workerConfigurations.put(workerIdCounter, remoteWorkerConfiguration)

            // increment id counter
            workerIdCounter = workerIdCounter + 1
          }
        }

        // deploy network services and remote infrastructure
        deployLeader

        // continue with coordinator normal bootstrap
        optionalCg = Some(super.boot)
    }
    
    optionalCg

  }
  
  /**
   * Start the necessary services for zombies to communicate with leader
   */
  def deployZombie {
    
    // start zombie manager
    remote.start(InetAddress.getLocalHost.getHostAddress, Constants.MANAGER_SERVICE_PORT)
    remote.register(Constants.ZOMBIE_MANAGER_SERVICE_NAME, actorOf(new ZombieManager(config.coordinatorAddress)))
    
  }

  /**
   * Start the necessary services for zombies to communicate with coordinator staying at the leader (worker infrastructure)
   * Also blocks until all workers have finished being instantiated
   */
  def deployLeader {

    // start coordinator forwarder which will receive coordinator messages
    remote.start(localIp, Constants.COORDINATOR_SERVICE_PORT)
    remote.register(Constants.COORDINATOR_SERVICE_NAME, actorOf[AkkaCoordinatorForwarder])

    // TODO: add here a blocking operation that asks the manager if everyone has joined, wait until everyone has

  }

  protected def createLogger: MessageRecipient[LogMessage] = new DefaultLogger

  def createWorkers(workerApi: WorkerApi) {

    // workerApi.createWorker(workerId).asInstanceOf[ActorRef]

    // TODO: correctly start remote workers
    // create, send message, get it back, signal OK

  }

  def createComputeGraph(workerApi: WorkerApi, coordinator: Coordinator): DefaultComputeGraph = {
    new DefaultComputeGraph(config, workerApi, coordinator)
  }

  def shutdown {

    // signal managers
    println("shutdown")
  }

}
