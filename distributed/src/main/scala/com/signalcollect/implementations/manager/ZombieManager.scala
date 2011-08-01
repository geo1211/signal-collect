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
import akka.dispatch._

import com.signalcollect.api.factory._
import com.signalcollect.implementations.messaging._
import com.signalcollect.interfaces._
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.util.Constants

import java.util.Date
import java.net.InetAddress

class ZombieManager(leaderIp: String) extends Manager with Actor {

  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  var config: DistributedConfiguration = _

  protected lazy val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

  // get leader hook
  val leader = remote.actorFor(Constants.LEADER_MANAGER_SERVICE_NAME, leaderIp, Constants.REMOTE_SERVER_PORT)

  // ask for config
  leader ! ConfigRequest

  def receive = {

    case Shutdown =>
      println("Zombie shutdown received at " + new Date)
      self.exit()

    // get configuration from leader
    case ConfigResponse(c) =>
      config = c
      createWorkers

  }

  def createWorkers {

    println("|||||||||||||||||||")
    println("||||| REMOTES |||||")
    println("|||||||||||||||||||")

    val nodeIpAddress = InetAddress.getLocalHost.getHostAddress

    // get only those workers that should be instantiated at this node 
    val workers = config.workerConfigurations.filter(x => x._2.ipAddress.equals(nodeIpAddress))

    // start the workers
    for (idConfig <- workers) {

      val workerId = idConfig._1

      val workerConfig = idConfig._2

      val workerFactory = workerConfig.workerFactory /*worker.AkkaRemoteWorker*/

      /*// debug
      if (!workerConfig.workerFactory.equals(worker.AkkaRemoteWorker))
        sys.error("ooops, remote worker factory should be used. check bootstrap/configuration setup")*/

      // create the worker with the retrieved configuration (ip,port), coordinator reference, and mapper
      val worker = workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, leader, mapper)

    } // end for each worker

    // tell the leader you are alive
    leader ! ZombieIsReady(nodeIpAddress)

  }
}