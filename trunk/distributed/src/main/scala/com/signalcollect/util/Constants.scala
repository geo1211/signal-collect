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

/**
 * Ports and service names used by the distributed system
 */
object Constants {

  // default port for all machines
  def REMOTE_SERVER_PORT = 2552

  def LEADER_NAME = "leader-service"
  def ZOMBIE_NAME = "zombie-service"
  def COORDINATOR_NAME = "coordinator-service"

  def BOOT_NAME = "boot-service"

  def WORKER_NAME = "worker-service"

  // timeout for getting a msg back
  def REPLY_TIMEOUT = 500l

}