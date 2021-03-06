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

package com.signalcollect.implementations.worker

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Actor._
import akka.dispatch._
import akka.actor.ReceiveTimeout
import akka.actor.PoisonPill

import java.util.Date

import com.signalcollect.implementations._
import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import com.signalcollect.implementations.coordinator.WorkerApi
import com.signalcollect.util._

class AkkaWorker(workerId: Int,
  workerConfig: WorkerConfiguration,
  numberOfWorkers: Int,
  coordinator: Any,
  mapper: VertexToWorkerMapper,
  loggingLevel: Int)
  extends LocalWorker(workerId, workerConfig, numberOfWorkers, coordinator, mapper, loggingLevel)
  with Actor {

  var zombieState = true

  /**
   * Starts the worker (puts it into a ready state for receiving messages)
   */
  override def initialize {
    self.start
  }

  override def postStop {
    println("Shutdown worker" + workerId)
    messageBus.sendToCoordinator("Shutdown")
  }

  /**
   * Akka dispatcher. This assigns one exclusive thread per worker (actor)
   */
  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  /**
   * Stops the worker execution
   */
  override def shutdown = {
    debug("WorkerId" + workerId + "=> shutdown received at " + new Date)
    self.stop
  }

  /**
   * Escape from processing of signals and collects
   * This is a way of making sure messages are processed as soon as they arrive
   */
  var processedAllLastTime = true

  /**
   * Timeout for akka actor idling (in milliseconds)
   */
  self.receiveTimeout = Some(250l)

  /**
   * This is method gets executed when the akka actor receives a message.
   * This method call is internally a "give me the first message from akka mailbox"
   */
  def receive = {

    case PoisonPill =>
      shutdown

    /**
     * ReceiveTimeout message only gets sent after akka actor mailbox has been empty for "receiveTimeout" milliseconds
     */
    case ReceiveTimeout =>

      if (!zombieState) {
        // idle handling
        if (isConverged || isPaused) { // if I have nothing to compute and the mailbox is empty, i'll be idle
          if (mailboxIsEmpty)
            setIdle(true)
        }
      }

    case "Hello" =>
      self.reply("ok")

    /**
     * Anything else
     */
    case msg =>

      zombieState = false
      setIdle(false)
      process(msg) // process the message
      handlePauseAndContinue
      performComputation

  }

  /**
   * This is where the computation gets done.
   * Basically, after a message has been processed, the worker will try to "get the job done" (signal and collect operations)
   */
  def performComputation = {

    // While the computation is in progress (work to do)
    if (!isPaused) {

      // alternately check the inbox and collect/signal
      while (mailboxIsEmpty && !isConverged) {

        // if nothing was left to be processed from last processing
        if (processedAllLastTime) {
          vertexStore.toSignal.foreach(executeSignalOperationOfVertex(_), true)	
          processedAllLastTime = vertexStore.toCollect.foreach(
            (vertexId, uncollectedSignals) => {

              val collectExecuted = executeCollectOperationOfVertex(vertexId, uncollectedSignals, false)
              if (collectExecuted) {
                executeSignalOperationOfVertex(vertexId)
              }
            }, true, () => !mailboxIsEmpty)
        } else
          processedAllLastTime = vertexStore.toCollect.foreach(
            (vertexId, uncollectedSignals) => {
              val collectExecuted = executeCollectOperationOfVertex(vertexId, uncollectedSignals, false)
              if (collectExecuted) {
                executeSignalOperationOfVertex(vertexId)
              }
            }, true, () => !mailboxIsEmpty)
      } // end while
    } // !isPaused

  }

  /**
   * Checks if the Actor mailbox is empty
   */
  def mailboxIsEmpty: Boolean = if (self == null) true else self.dispatcher.mailboxIsEmpty(self)

  /**
   * Just a check. Sending messages to Akka workers it should be done using the bang operator ( ! )
   */
  override def receive(message: Any) = sys.error("Receive should not be called from Akka Workers. This receive is not the same one from Akka.")

}