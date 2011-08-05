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

import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration.DistributedConfiguration

class LeaderManager(numberOfNodes: Int) extends Manager with Actor {

  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  var zombieRefs: List[ActorRef] = List()

  var nodesReady: Int = 0
  var nodesAlive: Int = 0

  var allReady = false
  var allAlive = false

  def receive = {

    case "Hello" =>
      self.reply("ok")

    case Config(c) =>
      zombieRefs foreach { x => x ! Config(c) }

    case Shutdown =>
      // shutdown all zombie managers, not needed anymore
      zombieRefs foreach { x => x ! Shutdown }
      self.stop

    // a zombie is ready (workers are instantiated)
    case ZombieIsReady(addr) =>

      println("zombie is ready = " + addr)

      // add node ready
      nodesReady = nodesReady + 1

    case CheckAllReady =>
      if (nodesReady == numberOfNodes - 1)
        allReady = true

      self.reply(allReady)

    // a zombie requested the configuration
    case ZombieIsAlive(addr) =>

      nodesAlive = nodesAlive + 1

      println("zombie " + addr + " is alive...")

      // add node joined
      val ref = self.sender.get
      zombieRefs = ref :: zombieRefs

    case CheckAllAlive =>
      if (nodesAlive == numberOfNodes - 1)
        allAlive = true

      self.reply(allAlive)

  }

}