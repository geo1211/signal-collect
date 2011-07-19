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
 * Generalization of worker configuration parameters for the worker constructor
 */
trait WorkerConfiguration {
  def workerFactory: WorkerFactory
  def messageBusFactory: MessageBusFactory
  def storageFactory: StorageFactory

  override def toString: String = {
    "worker factory" + "\t" + workerFactory + "\n" +
      "messagebus" + "\t" + messageBusFactory + "\n" +
      "storage" + "\t" + "\t" + storageFactory
  }

}

case class DefaultLocalWorkerConfiguration(
  workerFactory: WorkerFactory = worker.Local,
  messageBusFactory: MessageBusFactory = messageBus.SharedMemory,
  storageFactory: StorageFactory = storage.InMemory) extends WorkerConfiguration
