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

import com.signalcollect.api.factory._
import com.signalcollect.implementations.messaging._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.util.Constants

import java.net.InetAddress

class ZombieManager(masterIp: String) extends Manager with Actor {

  var config: DistributedConfiguration = _

  protected lazy val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

  // get master hook
  val master = remote.actorFor(Constants.MASTER_MANAGER_SERVICE_NAME, masterIp, Constants.MANAGER_SERVICE_PORT)

  // ask for config
  master ! ConfigRequest

  def receive = {

    // get configuration from master manager
    case ConfigResponse(c) =>
      config = c
      createWorkers

  }

  def shutdown = self.stop

  def createWorkers {

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
      val worker = workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, master, mapper)

    } // end for each worker

  }
}