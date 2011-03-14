/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package ch.uzh.ifi.ddis.signalcollect.implementations.coordinator

import ch.uzh.ifi.ddis.signalcollect.api.Queues._
import ch.uzh.ifi.ddis.signalcollect.api.Workers._
import ch.uzh.ifi.ddis.signalcollect.api.MessageBuses._
import ch.uzh.ifi.ddis.signalcollect.implementations.logging.SeparateThreadLogger
import ch.uzh.ifi.ddis.signalcollect.interfaces._
import java.util.concurrent.BlockingQueue


class AsynchronousCoordinator(
  numberOfWorkers: Int,
  workerFactory: WorkerFactory,
  messageInboxFactory: QueueFactory,
  messageBusFactory: MessageBusFactory,
  logger: Option[MessageRecipient[Any]] = None 
  ) extends SynchronousCoordinator(numberOfWorkers, workerFactory, messageInboxFactory, messageBusFactory, logger) {
	 
  override def performComputation: collection.mutable.Map[String, Any] = {
    executeComputationStep
    startComputation
    awaitStalledComputation
    collection.mutable.LinkedHashMap[String, Any]()
  }

}