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
import akka.actor.ActorRef
import akka.remoteinterface._
import akka.dispatch._

import java.util.Date

import com.signalcollect.interfaces._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration.DistributedConfiguration

class LeaderManager(config: DistributedConfiguration) extends Manager with Actor {
  
  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  var nodeCount = config.nodesAddress.size

  var zombieRefs: List[ActorRef] = List()

  var nodesJoined: List[String] = List()
  var nodesReady: List[String] = List()

  var allReady = false
  var allJoined = false

  def receive = {

    case Shutdown =>

      // shutdown all zombie managers, not needed anymore
      zombieRefs foreach { x => x ! Shutdown }

      println("Leader shutdown received at " + new Date)
      self.exit()

    // a zombie is ready (workers are instantiated)
    case ZombieIsReady(addr) =>
      // debug FIXME
      if (allReady)
        sys.error("oops, this shouldn't happen")

      // add node ready
      nodesReady = addr :: nodesReady

      val ref = self.sender.get
      zombieRefs = ref :: zombieRefs

    case CheckAllReady =>
      if (nodesReady.size == nodeCount - 1)
        allReady = true

      self.reply(allReady)

    // a zombie requested the configuration
    case ConfigRequest(addr) =>

      // debug FIXME
      if (allJoined)
        sys.error("oops, this shouldn't happen")

      // add node joined
      nodesJoined = addr :: nodesJoined

      // book keeping
      if (nodesJoined.size == nodeCount - 1)
        allJoined = true

      // send back configuration
      self.reply(ConfigResponse(config))

    case CheckAllJoined =>
      self.reply(allJoined)

  }

}