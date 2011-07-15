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

package signalcollect.api

import signalcollect.interfaces._
import signalcollect.configuration._
import signalcollect.implementations.coordinator._
import signalcollect.implementations.logging._

/**
 * The bootstrap sequence for initializing the distributed infrastructure 
 */
class DistributedBootstrap(val config: Configuration) extends Bootstrap {

  override def boot: ComputeGraph = {
    deploy
    super.boot
  }

  def deploy {
    // start managers
  }
  
  protected def createLogger: MessageRecipient[LogMessage] = new DefaultLogger

  def createWorkers(workerApi: WorkerApi) {

    val workersPerNode = config.asInstanceOf[DistributedConfiguration].nodeProvisioning.workerPorts

    // TODO: correctly start remote workers
    // TODO: add supervising hierarchical layer for knowing whether a worker is alive or not
    
/*    def createAkkaWorker(workerId: Int, workerConfiguration: RemoteWorkerConfiguration) {

    val workerFactory = Factory.Worker.Akka

    // create the worker configuration for remote worker instantiation
    config.asInstanceOf[DistributedConfiguration].workerConfigurations.put(workerId, workerConfiguration)

    // create the worker
    val worker = workerFactory.createInstance(workerId, config, this, mapper)

    // put it to the array of workers
    workers(workerId) = worker

  }*/

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
//class Bootstrap(bootstrapConfig: BootstrapConfiguration) {

  /**
   *
   * What do I need to start the execution?
   *
   * 1. execute location
   * 	1.1 local
   * 	1.2 kraken
   * 2. execution architecture
   * 	2.1 local
   * 	2.2 distributed
   * 		2.2.1 resource allocation config
   * 3. ComputeGraphBuilder
   * 	3.1 LocalComputeGraph (?)
   * 	3.2 DistributedComputeGraph
   *
   * 	3.1.1 & 3.2.1 Worker Initialization
   * 		Local Initialization or Distributed Worker Initialization
   *
   * 4. Job configuration
   * 	4.1 algorithm
   * 	4.2 results mode
   * 		4.2.1 local print + csv
   * 		4.2.2 spreadsheet config
   * 5. configuration object for compute graph
   *
   */

/*} Managers stuff below

class LocalBootstrap(bootstrapConfig: BootstrapConfiguration) extends Bootstrap(bootstrapConfig) {*/

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

*/
  /**
   * Serialization is using Java serialization
   *
   * TODO: Add JSON serialization, seems to be the only one that supports 'any'
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
