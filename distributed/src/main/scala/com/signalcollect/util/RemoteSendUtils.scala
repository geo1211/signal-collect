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

package com.signalcollect.util

import com.signalcollect.util.Constants._
import akka.actor.ActorRef

trait RemoteSendUtils {

  def checkAlive(to: ActorRef) {
    checkAliveWithMsg(to, "Hello")
  }

  def checkAliveWithMsg(to: ActorRef, msg: Any) {

    val timeout = 500L

    val result: Option[Any] = to !! (msg, timeout)

    result match {
      case Some(reply) =>
      //println(reply)
      case None =>
        println("timeout at check")
        sys.error("No reply within " + timeout + " ms")
    }

  }

  def checkAliveWithRetry(to: ActorRef, retries: Int) {
    sendWithRetry(to, "Hello", retries)
  }

  def sendWithRetry(to: ActorRef, msg: Any, retries: Int) {

    var attempts = retries

    var retry = true

    val timeout = 500L

    while (retry) {
      val result: Option[Any] = to !! (msg, timeout)

      result match {
        case Some(reply) =>
          retry = false
        case None =>
          attempts = attempts - 1
          if (attempts < -1)
            sys.error("No reply within " + timeout + " ms after " + retries + " attempts...")
      }
    }

  }
}