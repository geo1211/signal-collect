/*
 *  @author Philip Stutz
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

package com.signalcollect.implementations.coordinator

import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import com.signalcollect.implementations.messaging._
import com.signalcollect.api.factory._
import com.signalcollect.util._

import akka.actor.Actor._
import akka.actor.ActorRef

class AkkaWorkerApi(config: Configuration, logger: MessageRecipient[LogMessage]) extends WorkerApi(config, logger) {

  override def toString = "AkkaWorkerApi"

  override protected def createWorkerProxies: Array[Worker] = {
    val workerProxies = new Array[Worker](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val workerProxy = AkkaWorkerProxy.create(workerId, workerProxyMessageBuses(workerId))
      workerProxies(workerId) = workerProxy
    }
    workerProxies
  }

  override protected def createWorkerProxyMessageBuses: Array[MessageBus[Any]] = {
    val workerProxyMessageBuses = new Array[MessageBus[Any]](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val proxyMessageBus = config.workerConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
      proxyMessageBus.registerCoordinator(RemoteWorkerInfo(ipAddress = config.asInstanceOf[DistributedConfiguration].leaderAddress, serviceName = Constants.COORDINATOR_SERVICE_NAME))
      workerProxyMessageBuses(workerId) = proxyMessageBus
    }
    workerProxyMessageBuses
  }

  override def initialize {

    if (!isInitialized) {
      Thread.currentThread.setName("Coordinator")
      messageBus.registerCoordinator(RemoteWorkerInfo(ipAddress = config.asInstanceOf[DistributedConfiguration].leaderAddress, serviceName = Constants.COORDINATOR_SERVICE_NAME))
      workerProxyMessageBuses foreach (_.registerCoordinator(RemoteWorkerInfo(ipAddress = config.asInstanceOf[DistributedConfiguration].leaderAddress, serviceName = Constants.COORDINATOR_SERVICE_NAME)))
      for (workerId <- 0 until config.numberOfWorkers) {
        val workerConfig = config.asInstanceOf[DistributedConfiguration].workerConfigurations.get(workerId).get
        messageBus.registerWorker(workerId, RemoteWorkerInfo(ipAddress = workerConfig.ipAddress, serviceName = workerConfig.serviceName))
        workerProxyMessageBuses foreach (_.registerWorker(workerId, RemoteWorkerInfo(ipAddress = workerConfig.ipAddress, serviceName = workerConfig.serviceName)))
      }

      for (workerId <- 0 until config.numberOfWorkers) {
        val workerConfig = config.asInstanceOf[DistributedConfiguration].workerConfigurations.get(workerId).get
        workerProxies foreach (_.registerWorker(workerId, RemoteWorkerInfo(ipAddress = workerConfig.ipAddress, serviceName = workerConfig.serviceName)))
      }
      isInitialized = true
    }
  }

  override def shutdown = {

    parallelWorkerProxies foreach (x => x.shutdown)

    registry.shutdownAll

    remote.shutdown

  }

}