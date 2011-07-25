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

package com.signalcollect

import org.clapper.argot._
import org.clapper.argot.ArgotConverters._

import akka.actor.Actor
import akka.actor.Actor._

import com.signalcollect.util.Constants
import com.signalcollect.implementations.manager.ZombieManager

import java.net.InetAddress

object Zombie {

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    val masterIp = args(0)

    // start local manager
    remote.start(InetAddress.getLocalHost.getHostAddress, Constants.MANAGER_SERVICE_PORT)
    remote.register(Constants.ZOMBIE_MANAGER_SERVICE_NAME, actorOf(new ZombieManager(masterIp)))

  }

}