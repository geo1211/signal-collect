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

import com.signalcollect.api.factory._
import com.signalcollect.interfaces._

/**
 * The NodeProvisioning class will be responsible for providing these values for each worker
 */
trait RemoteWorkerConfiguration extends WorkerConfiguration {
  def ipAddress: String
  def port: Int
}

/**
 * Used by ZOMBIES
 */
case class DefaultRemoteWorkerConfiguration(
  workerFactory: WorkerFactory = worker.AkkaRemoteWorker,
  messageBusFactory: MessageBusFactory = messageBus.AkkaBus,
  messageInboxLimits: Option[(Int, Int)] = None, //Some(50, 1000), 
  storageFactory: StorageFactory = storage.InMemory,
  ipAddress: String = "",
  port: Int = 0) extends RemoteWorkerConfiguration

/**
 * Used by the COORDINATOR
 */
case class DefaultRemoteWorkerReferenceConfiguration(
  workerFactory: WorkerFactory = worker.AkkaRemoteReference,
  messageBusFactory: MessageBusFactory = messageBus.AkkaBus,
  messageInboxLimits: Option[(Int, Int)] = None, //Some(50, 1000), 
  storageFactory: StorageFactory = storage.InMemory) extends WorkerConfiguration
