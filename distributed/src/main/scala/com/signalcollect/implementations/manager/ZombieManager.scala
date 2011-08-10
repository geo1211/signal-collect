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

import com.signalcollect.api.factory._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration._
import com.signalcollect.util._
import com.signalcollect.util.Constants._

import java.net.InetAddress

class ZombieManager(leaderIp: String) extends Manager with Actor with BootstrapHelper {

  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  var config: DistributedConfiguration = _

  var leader: ActorRef = _

  override def preStart {

    val timeout = 500l

    // get leader hook
    leader = remote.actorFor(LEADER_MANAGER_SERVICE_NAME, leaderIp, REMOTE_SERVER_PORT)

    checkAliveWithRetry(leader, 5)
  }

  def receive = {

    case SendAlive =>
      leader ! ZombieIsAlive(localIp)

    case Shutdown =>
      self.stop

    // get configuration from leader
    case Config(c) =>
      config = c

      println("||||||||||||||||||||||||||")
      println("||||| ZOMBIE WORKERS |||||")
      println("||||||||||||||||||||||||||")
      
      val coordinatorForwarder = remote.actorFor(Constants.COORDINATOR_SERVICE_NAME, config.leaderAddress, Constants.REMOTE_SERVER_PORT)

      createLocalWorkers(coordinatorForwarder, config)
      //println("Zombie finished workers")

      // tell the leader you are alive
      leader ! ZombieIsReady(localIp)

  }

}