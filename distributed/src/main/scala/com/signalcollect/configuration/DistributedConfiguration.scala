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

import com.signalcollect.interfaces._
import com.signalcollect.configuration.provisioning._

import java.util.HashMap

/**
 * Configuration for the distributed execution of signal/collect
 */
trait DistributedConfiguration extends Configuration {

  def userName: String

  def numberOfNodes: Int

  def nodesAddress: List[String]

  def coordinatorAddress: String

  def nodeProvisioning: NodeProvisioning

  /**
   * The difference now is that the distributed architecture requires that workers have ports and addresses
   * This information is only used for initializing workers
   */
  def workerConfigurations: HashMap[Int, WorkerConfiguration] = new HashMap[Int, WorkerConfiguration]()

}

case class DefaultDistributedConfiguration(
  userName: String = System.getProperty("user.name"),
  numberOfWorkers: Int = Runtime.getRuntime.availableProcessors,
  customLogger: Option[MessageRecipient[LogMessage]] = None,
  executionArchitecture: ExecutionArchitecture = DistributedExecutionArchitecture,
  executionConfiguration: ExecutionConfiguration = DefaultExecutionConfiguration,
  workerConfiguration: WorkerConfiguration = DefaultRemoteWorkerConfiguration(),
  numberOfNodes: Int = 1,
  nodesAddress: List[String] = List("localhost"),
  coordinatorAddress: String = "localhost",
  nodeProvisioning: NodeProvisioning = new EqualNodeProvisioning(Vector("localhost"), Runtime.getRuntime.availableProcessors)) extends DistributedConfiguration