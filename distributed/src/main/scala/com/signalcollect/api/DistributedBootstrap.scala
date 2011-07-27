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
import com.signalcollect.implementations.coordinator._
import com.signalcollect.implementations.logging._
import com.signalcollect.implementations.messaging._
import com.signalcollect.util._

import akka.actor.Actor
import akka.actor.Actor._
import akka.remoteinterface._

import java.net.InetAddress

/**
 * The bootstrap sequence for initializing the distributed infrastructure
 */
class DistributedBootstrap(val config: DistributedConfiguration) extends Bootstrap {

  /**
   * Get per worker the Ip address of the machine where it will be instantiated + the port where it will listen
   * Add to that the configuration necessary for remote initialization
   */
  override def boot: ComputeGraph = {

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
    deploy
    
    // continue with coordinator normal bootstrap
    super.boot
    
  }

  /**
   * Start the necessary services for remote deploy of workers and communication with the coordinator
   */
  def deploy {

    val coordinatorIp = InetAddress.getLocalHost.getHostAddress

    // start manager for talking to remote nodes
    Actor.remote.start(coordinatorIp, Constants.MANAGER_SERVICE_PORT)
    Actor.remote.register(Constants.MASTER_MANAGER_SERVICE_NAME, actorOf[AkkaCoordinatorForwarder])

    // start coordinator forwarder which will receive coordinator messages
    Actor.remote.start(coordinatorIp, Constants.COORDINATOR_SERVICE_PORT)
    Actor.remote.register(Constants.COORDINATOR_SERVICE_NAME, actorOf[AkkaCoordinatorForwarder])

    /**
     *  TODO: do you want to run workers in the coordinator node?
     *  
     *  well, I guess if one puts the coordinator's ip in the nodesAddress, then yes
     */
    //val nodesWithWorkers = config.nodesAddress.filter(x => !x.equals(coordinatorIp))
    val nodesWithWorkers = config.nodesAddress

    /*// prepare the jar
    val helper = new ZombieJarHelper(config.numberOfWorkers, config.userName)

    // copy the jar to hosts
    helper.copyJarToHosts(nodesWithWorkers)

    // in each computation node
    nodesWithWorkers.foreach {
      host =>

        // start zombie at remote node 
        helper.startJarAtHost(host, coordinatorIp)
    }*/

    // TODO: add here a blocking operation that asks the manager if everyone has joined, wait until everyone has

  }

  protected def createLogger: MessageRecipient[LogMessage] = new DefaultLogger

  def createWorkers(workerApi: WorkerApi) {

    // workerApi.createWorker(workerId).asInstanceOf[ActorRef].start

    // TODO: correctly start remote workers
    // create, send message, get it back, signal OK

  }

  def createComputeGraph(workerApi: WorkerApi, coordinator: Coordinator): DefaultComputeGraph = {
    null
  }

  def shutdown {

    // signal managers
    println("shutdown")
  }

}

/**
 * args(x)
 *  0 = initial type (master or zombie)
 *  1 = ipAddress of coordinator
 */

  /*  val numberOfNodes = args.size - 3
  
  val workersPerNode = new Array[Int](numberOfNodes)
  
  if (args(0) equals "master") {
    
    if ( == 1 ) {
      
      
       *  local workers, need to check if when creating remote workers in only one node, they get created as local actors
       *  
       *  this is the case in Akka 2.0, local actors and remotes are treated the same
       
      
    }
    else {
      
      val numberOfWorkers = ((2)).asInstanceOf[]
  
	  // equal division of workers among all nodes, eg. 25 workers in 3 nodes = 9 on the first, 8 on the other 2
	  val div: numberOfWorkers.asInstanceOf[] / .asInstanceOf[]
	
	  for (i <- (i <- 0 to numberOfNodes {
	    if (i == 0)
	      workersPerNode worker(i, = .ceil(div).asInstanceOf[])
	    else
	      workersPerNode worker(i, = .floor(div).asInstanceOf[]))
	  }

		
		 *  manager (actor) start
		 *  It will manage the rendezvous from the created zombies
		 
		.remote.start( 2552)
		.remote.register("master", (new MasterManager()))
		
		// ssh start zombies with zombie and reference to the master
    
    }

  } else if ((0) equals "zombie") {

    */
  /**
   * manager (actor) start
   * It will rendezvous with master
   */ /*
    .remote.start( 2552)
    .remote.register("master", (new ZombieManager((1)))) // args(1) contains address for master

  }

}r("master", actorOf(new ZombieManager(args(1)))) // args(1) contains address for master

  }

}

   */ /*

case class Hello(ipAddress: String)

case class InitializeWorkers(messageBus: MessageBus[Any, Any])

case class WorkersUp

case class Terminate

trait Manager extends Actor with Logging {

  def receive = {
    case x => process(x)
  }

  def process(msg: Any) {

    msg match {

      case Hello(ip) =>
        processHello(ip)

      case Terminate =>
        processTerminate

      case x =>

    }
  }

  */
  /**
   * Generic send message
   */ /*
  def sendCommand(cmd: Any, dest: ActorRef) {
    dest ! cmd
  }

  def processHello(ip: String)

  def processTerminate {
    self.stop()
  }

}

class ZombieManager(masterIp: String) extends Manager {

  // get remote hook for master manager
  val masterRef: ActorRef = Actor.remote.actorFor("master", masterIp, 2552)

  override def process(msg: Any) {
    msg match {
      case InitializeWorkers(mb) =>
        initializeWorkers(mb)
      case x =>
        super.process(x)
    }

  }

  def initializeWorkers(messageBus: MessageBus[Any, Any]) {
    println("blablabla")
    sendCommand(WorkersUp, masterRef)
  }

  def processHello(ip: String) {}

}

class MasterManager(numberOfNodes: Int) extends Manager {

  // holds all remote manager references
  var remoteManagers: Map[String, ActorRef] = _

  var checkedIn = 0
  
  override def process(msg: Any) {
    msg match {
      case WorkersUp =>
        checkedIn += 1
        
        if (checkedIn == numberOfNodes)
          startExecution
        
      case x =>
        super.process(x)
    }

  }

  def processHello(ip: String) {
    // get remote hook
    val actorRef = Actor.remote.actorFor("zombie", ip, 2552)
    // save reference
    remoteManagers += (ip -> actorRef)

  }

  override def processTerminate {
    remoteManagers.foreach { x => x._2 ! Terminate }
    super.processTerminate
  }
  
  def startExecution {
    
    // new default graph blabla, workerfactory, messagebus
    
  }*/

//}
