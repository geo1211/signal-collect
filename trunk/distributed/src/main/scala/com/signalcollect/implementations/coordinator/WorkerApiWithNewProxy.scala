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
import com.signalcollect.implementations.graph.DefaultGraphApi
import com.signalcollect.api.factory._

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import scala.collection.parallel.mutable.ParArray
import scala.collection.JavaConversions._

import akka.actor.Actor._
import akka.actor.ActorRef
import akka.serialization.RemoteActorSerialization._

class WorkerApiWithNewProxy(config: Configuration, logger: MessageRecipient[LogMessage]) extends WorkerApi(config, logger) {

  override def toString = "WorkerApiWithNewProxy"

  override protected lazy val workerProxies: Array[Worker] = createWorkerProxies

  override protected def createWorkerProxies: Array[Worker] = {
    val workerProxies = new Array[Worker](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val workerProxy = WorkerProxyWithSerialization.create(workerId, workerProxyMessageBuses(workerId))
      workerProxies(workerId) = workerProxy
    }
    workerProxies
  }

  var coordinator: ActorRef = _

  override protected def createWorkerProxyMessageBuses: Array[MessageBus[Any]] = {
    val workerProxyMessageBuses = new Array[MessageBus[Any]](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val proxyMessageBus = config.workerConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
      proxyMessageBus.registerCoordinator(coordinator)
      workerProxyMessageBuses(workerId) = proxyMessageBus
    }
    workerProxyMessageBuses
  }

  /*  def initialize(coord: Any) {
    
    coordinator = coord.asInstanceOf[ActorRef]
    
    if (!isInitialized) {
      Thread.currentThread.setName("Coordinator")
      messageBus.registerCoordinator(coordinator)
      workerProxyMessageBuses foreach (_.registerCoordinator(coordinator))
      for (workerId <- 0 until config.numberOfWorkers) {
        messageBus.registerWorker(workerId, workers(workerId))
        workerProxyMessageBuses foreach (_.registerWorker(workerId, workers(workerId)))
      }
      for (workerId <- 0 until config.numberOfWorkers) {
        workerProxies foreach (_.registerWorker(workerId, workers(workerId)))
      }
      isInitialized = true
    }
  }*/

  def initialize(coord: Any) {

    coordinator = coord.asInstanceOf[ActorRef]

    if (!isInitialized) {
      Thread.currentThread.setName("Coordinator")
      messageBus.registerCoordinator(coordinator)
      workerProxyMessageBuses foreach (_.registerCoordinator(coordinator))
      for (workerId <- 0 until config.numberOfWorkers) {
        messageBus.registerWorker(workerId, workers(workerId))
        workerProxyMessageBuses foreach (_.registerWorker(workerId, workers(workerId)))
      }
      for (workerId <- 0 until config.numberOfWorkers) {
        workerProxies foreach (_.registerWorker(workerId, toRemoteActorRefProtocol(workers(workerId).asInstanceOf[ActorRef]).toByteArray))
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