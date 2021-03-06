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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import com.signalcollect.interfaces._
import com.signalcollect.implementations.messaging.AbstractMessageRecipient
import java.lang.reflect.Method

object WorkerProxy {

  protected val workerClass = classOf[Worker]

  def create(workerId: Int, messageBus: MessageBus[Any], loggingLevel: Int): Worker = {
    Proxy.newProxyInstance(
      workerClass.getClassLoader,
      Array[Class[_]](classOf[Worker]), //workerClass.getInterfaces,
      new WorkerProxy(workerId, messageBus)).asInstanceOf[Worker]
  }

}

/**
 *  Proxy for worker functions.
 *
 *  Only works when there is at most 1 client thread calling an instance of this class.
 *  Only works if the transport used is reliable and if workers never fail.
 */
class WorkerProxy(val workerId: Int, val messageBus: MessageBus[Any]) extends InvocationHandler {

  protected def relay(command: Worker => Unit) = messageBus.sendToWorker(workerId, WorkerRequest(command))

  override def toString = "WorkerProxy" + workerId

  var workerMessage: Option[WorkerReply] = null
  val monitor = new Object

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    var returnValue: Object = null
    // intercept method named "receive"
    if ("receive".equals(method.getName)) {
      val message = arguments(0).asInstanceOf[WorkerReply]
      if (message != null) {
        workerMessage = Some(message)
      } else {
        workerMessage = None
      }
      monitor.synchronized {
        monitor.notify
      }
    } else {
      val command = { worker: Worker =>
        worker.debug(method, arguments: _*)
        val result = method.invoke(worker, arguments: _*)
        val reply = WorkerReply(worker.workerId, result)
        worker.messageBus.sendToCoordinator(reply)
      }
      relay(command)

      /**
       *  Blocking operation, until receive of worker reply by coordinator
       *  The reply will trigger the invoke again with receive method
       */
      if (workerMessage == null) {
        monitor.synchronized {
          while (workerMessage == null) {
            monitor.wait(10)
          }
        }
      }
      if (workerMessage.isDefined) {
        returnValue = workerMessage.get.result.asInstanceOf[AnyRef]
      }
      workerMessage = null
    }
    returnValue
  }

}