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