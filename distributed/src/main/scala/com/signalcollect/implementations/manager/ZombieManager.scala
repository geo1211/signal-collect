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

package com.signalcollect.implementations.manager

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.dispatch._

import com.signalcollect.api.factory._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.util._
import com.signalcollect.util.Constants._
import com.signalcollect.implementations.messaging.DefaultVertexToWorkerMapper

import java.util.Date
import java.net.InetAddress

class ZombieManager(leaderIp: String) extends Manager with Actor with RemoteSendUtils {

  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  val localIp = InetAddress.getLocalHost.getHostAddress

  var config: DistributedConfiguration = _

  var leader: ActorRef = _

  override def preStart {

    val timeout = 500l

    // get leader hook
    leader = remote.actorFor(LEADER_MANAGER_SERVICE_NAME, leaderIp, REMOTE_SERVER_PORT)

    checkAlive(leader)
  }

  def receive = {

    case SendAlive =>
      leader ! ZombieIsAlive(localIp)

    case Shutdown =>
      self.stop

    // get configuration from leader
    case Config(c) =>
      println("got config")
      config = c
      createWorkers

  }

  def createWorkers {

    println("|||||||||||||||||||")
    println("||||| REMOTES |||||")
    println("|||||||||||||||||||")

    val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

    // get only those workers that should be instantiated at this node 
    val workers = config.workerConfigurations.filter(x => x._2.ipAddress.equals(localIp))
    
    println("loop will start")

    workers foreach {
      x =>
        println("ID = " + x._1 + "\nconfig: = " + x._2.ipAddress + " name = " + x._2.serviceName)
    }

    // start the workers
    for (idConfig <- workers) {

      val workerId = idConfig._1

      val workerConfig = idConfig._2

      //println(workerId)

      val workerFactory = workerConfig.workerFactory //worker.AkkaRemoteWorker

      // debug
      /*if (!workerConfig.workerFactory.equals(worker.AkkaRemoteWorker))
        sys.error("ooops, remote worker factory should be used. check bootstrap/configuration setup")*/

      // create the worker with the retrieved configuration (ip,port), coordinator reference, and mapper
      workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, leader, mapper)

      val worker = remote.actorFor(workerConfig.asInstanceOf[RemoteWorkerConfiguration].serviceName, workerConfig.asInstanceOf[RemoteWorkerConfiguration].ipAddress, Constants.REMOTE_SERVER_PORT)

      checkAlive(worker)

      println("check success ID= " + workerId)

    } // end for each worker

    println("Zombie finished workers")

    // tell the leader you are alive
    leader ! ZombieIsReady(localIp)

  }
}