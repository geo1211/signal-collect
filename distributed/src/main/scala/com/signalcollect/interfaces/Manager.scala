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
   * The configuration will allow a Zombie to instantiate remote workers
   */
  case class Config(config: DistributedConfiguration) extends ManagerMessage

  case class CheckAllReady extends ManagerMessage
  case class CheckAllAlive extends ManagerMessage

  /**
   * It is a way to tell the master manager that all workers have been instantiated
   */
  case class ZombieIsReady(addr: String) extends ManagerMessage
  
  case class ZombieIsAlive(addr: String) extends ManagerMessage
  
  case class SendAlive extends ManagerMessage

  case class CoordinatorReference(coordinator: Any) extends ManagerMessage
  
  case class Shutdown extends ManagerMessage
}