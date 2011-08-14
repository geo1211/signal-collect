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
import com.signalcollect.api._
import java.lang.reflect.Method
import akka.actor.ActorRef

object AkkaWorkerProxy {

  protected val workerClass = classOf[Worker]

  def create(workerId: Int, messageBus: MessageBus[Any], loggingLevel: Int): Worker = {
    Proxy.newProxyInstance(
      workerClass.getClassLoader,
      Array[Class[_]](classOf[Worker]), //workerClass.getInterfaces,
      new AkkaWorkerProxy(workerId, messageBus, loggingLevel)).asInstanceOf[Worker]
  }

}

/*
 * Synchronous proxy for worker functions.
 * 
 * Only works when there is at most 1 client thread calling an instance of this class.
 * Only works if the transport used is reliable and if workers never fail.
 * 
 * This is mainly an architectural place holder until we find a proper RPC solution to use
 * with our message bus as the transport. 
 */
class AkkaWorkerProxy(val workerId: Int, val messageBus: MessageBus[Any], val loggingLevel: Int) extends InvocationHandler with Logging {

  protected def relay(command: Worker => Unit) = messageBus.sendToWorker(workerId, WorkerRequest(command))

  override def toString = "AkkaWorkerProxy" + workerId

  var workerMessage: Option[WorkerReply] = null
  val monitor = new Object

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    var returnValue: Object = null
    if ("receive".equals(method.getName)) {
      // intercept method named "receive"
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
      debug("Worker" + workerId + "." + method.getName)

      val methodParameters = method.getParameterTypes()
      val methodName = method.getName

      val command = { worker: Worker =>
        var result = worker.getClass.getMethod(methodName, methodParameters: _*).invoke(worker, arguments: _*)
        worker.messageBus.sendToCoordinator(WorkerReply(worker.workerId, result))
      }
      relay(command)

      /*
       * Blocking operation, until receive of worker reply by coordinator
       * The reply will trigger the invoke again with receive method
       */
      if (workerMessage == null) {
        monitor.synchronized {

          var c = 0

          while (workerMessage == null && c < 3000) {
            monitor.wait(10)
            c = c + 1
          }

          if (c >= 3000)
            throw new Exception("No response from worker ID = " + workerId + " within 30 secs")

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