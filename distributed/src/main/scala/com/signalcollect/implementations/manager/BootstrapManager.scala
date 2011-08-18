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

package com.signalcollect.implementations.manager

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.dispatch._

import java.util.Date

import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration.DistributedConfiguration
import com.signalcollect.util._

import scala.collection.mutable.HashMap

/**
 * Deals with messages related to the bootstrap and leader election mechanism
 *
 */
class BootstrapManager(numberOfNodes: Int) extends Manager with Actor {

  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  def localIp = java.net.InetAddress.getLocalHost.getHostAddress

  val map: HashMap[String, Long] = new HashMap[String, Long]

  var numIdsReceived = 0

  val myId = scala.util.Random.nextLong.abs

  var leaderIp = "undecided"

  def receive = {

    /**
     * send all machines my Ip address
     */
    case MachinesAddress(ipAddressList: List[String]) =>

      ipAddressList foreach { ip =>

        if (!ip.equals(localIp)) {

          val remoteMachine = remote.actorFor(Constants.BOOT_NAME, ip, Constants.REMOTE_SERVER_PORT)

          remoteMachine ! Id(localIp, myId)
        }

      }

    // received an Id from another machine, calculate who's leader when received all ids
    case Id(from: String, id: Long) =>

      numIdsReceived = numIdsReceived + 1
      map.put(from, id)

      if (numIdsReceived == numberOfNodes - 1) {

        map.put(localIp, myId)

        val leaderId = map.foldLeft(Long.MaxValue)((min, kv) => scala.math.min(min, kv._2))

        map.foreach(x => if (x._2 == leaderId) leaderIp = x._1)

      }

    // poll to check if leader ip has been defined
    case RequestLeaderIp =>
      self.reply(leaderIp)

  }

}