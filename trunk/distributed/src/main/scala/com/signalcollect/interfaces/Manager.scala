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

package com.signalcollect.interfaces

import com.signalcollect.configuration.DistributedConfiguration

trait Manager
/**
 * Companion object declaring messages
 */
object Manager {
  /**
   * Messages exchanged between managers only
   */
  sealed trait ManagerMessage
  /**
   * A zombie-instantiated worker (remote worker) message requesting for configuration parameters
   * It is also a way to rendezvous with the master manager
   */
  case class ConfigRequest(addr: String) extends ManagerMessage
  /**
   * The response to the configuration request from the master manager
   * The configuration will allow a Zombie to instantiate remote workers
   */
  case class ConfigResponse(config: DistributedConfiguration) extends ManagerMessage

  case class CheckAllReady
  case class CheckAllJoined

  /**
   * It is a way to tell the master manager that all workers have been instantiated
   */
  case class ZombieIsReady(addr: String)

  case class CoordinatorReference(coordinator: Any)
}