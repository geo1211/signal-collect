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

package com.signalcollect.configuration

import com.signalcollect.api.factory._
import com.signalcollect.interfaces._
import scala.collection.mutable.HashMap

/**
 * Configuration for the distributed execution of signal/collect
 */
trait DistributedConfiguration extends Configuration {

  def userName: String

  def numberOfMachines: Int

  def machinesAddress: List[String]

  def leaderAddress: String

  def provisionFactory: ProvisionFactory

  /**
   * The difference now is that the distributed architecture requires that workers have ports and addresses
   * Used both at the zombie and coordinator
   */
  def workerConfigurations: HashMap[Int, RemoteWorkerConfiguration]

}

case class DefaultDistributedConfiguration(
  userName: String = System.getProperty("user.name"),
  numberOfWorkers: Int = Runtime.getRuntime.availableProcessors,
  loggingLevel: Int = LoggingLevel.Warning,
  customLogger: Option[MessageRecipient[LogMessage]] = None,
  executionConfiguration: ExecutionConfiguration = DefaultExecutionConfiguration,
  workerConfiguration: WorkerConfiguration = DefaultRemoteWorkerReferenceConfiguration(),
  maxInboxSize: Option[Long] = None, //Some(Runtime.getRuntime.availableProcessors*5000),
  numberOfMachines: Int = 1,
  var machinesAddress: List[String] = List("localhost"),
  var leaderAddress: String = "localhost",
  provisionFactory: ProvisionFactory = provision.EqualProvisioning,
  var workerConfigurations: HashMap[Int, RemoteWorkerConfiguration] = new HashMap[Int, RemoteWorkerConfiguration]()) extends DistributedConfiguration