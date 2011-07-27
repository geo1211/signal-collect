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

import com.signalcollect.api._
import com.signalcollect.interfaces._
import com.signalcollect.configuration.provisioning._

object DefaultDistributedComputeGraphBuilder extends DistributedComputeGraphBuilder

/**
 * Builder for the creation of a compute graph needs a configuration object for the creation.
 * If the user passes a configuration object but then uses a method of this class, the configuration's object
 * parameter gets overriden ("inserted" in the config object) by the method call's parameter which was passed.
 */
class DistributedComputeGraphBuilder(protected val config: DistributedConfiguration = new DefaultDistributedConfiguration) extends Serializable {

  def build: Option[ComputeGraph] = new DistributedBootstrap(config).boota

  /**
   * Common configuration
   */
  def withNumberOfWorkers(newNumberOfWorkers: Int) = newRemoteBuilder(numberOfWorkers = newNumberOfWorkers)

  def withLogger(logger: MessageRecipient[LogMessage]) = newRemoteBuilder(customLogger = Some(logger))

  def withExecutionConfiguration(newExecutionConfiguration: ExecutionConfiguration) = newRemoteBuilder(executionConfiguration = newExecutionConfiguration)

  /**
   * Worker configuration
   */
  def withWorkerFactory(newWorkerFactory: WorkerFactory) = newRemoteBuilder(workerFactory = newWorkerFactory)

  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newRemoteBuilder(messageBusFactory = newMessageBusFactory)

  def withStorageFactory(newStorageFactory: StorageFactory) = newRemoteBuilder(storageFactory = newStorageFactory)

  /**
   * Distributed configuration
   */
  def withUserName(newUserName: String) = newRemoteBuilder(userName = newUserName)

  def withNumberOfNodes(newNumberOfNodes: Int) = newRemoteBuilder(numberOfNodes = newNumberOfNodes)

  def withNodesAddress(newNodesAddress: List[String]) = newRemoteBuilder(nodesAddress = newNodesAddress)

  def withCoordinatorAddress(newCoordinatorAddress: String) = newRemoteBuilder(coordinatorAddress = newCoordinatorAddress)

  def withNodeProvisioningFactory(newProvisionFactory: ProvisionFactory) = newRemoteBuilder(provisionFactory = newProvisionFactory)

  /**
   * Builds distributed compute graph
   */
  def newRemoteBuilder(
    userName: String = config.asInstanceOf[DistributedConfiguration].userName,
    numberOfWorkers: Int = config.numberOfWorkers,
    customLogger: Option[MessageRecipient[LogMessage]] = config.customLogger,
    workerFactory: WorkerFactory = config.workerConfiguration.workerFactory,
    messageBusFactory: MessageBusFactory = config.workerConfiguration.messageBusFactory,
    storageFactory: StorageFactory = config.workerConfiguration.storageFactory,
    executionConfiguration: ExecutionConfiguration = config.executionConfiguration,
    numberOfNodes: Int = config.asInstanceOf[DistributedConfiguration].numberOfNodes,
    nodesAddress: List[String] = config.asInstanceOf[DistributedConfiguration].nodesAddress,
    coordinatorAddress: String = config.asInstanceOf[DistributedConfiguration].coordinatorAddress,
    provisionFactory: ProvisionFactory = config.asInstanceOf[DistributedConfiguration].provisionFactory): DistributedComputeGraphBuilder = {
    new DistributedComputeGraphBuilder(
      DefaultDistributedConfiguration(
        userName = userName,
        numberOfWorkers = numberOfWorkers,
        customLogger = customLogger,
        executionConfiguration = executionConfiguration,
        workerConfiguration = DefaultRemoteWorkerReferenceConfiguration(
          workerFactory = workerFactory,
          messageBusFactory = messageBusFactory,
          storageFactory = storageFactory),
        numberOfNodes = numberOfNodes,
        nodesAddress = nodesAddress,
        coordinatorAddress = coordinatorAddress,
        provisionFactory = provisionFactory))

  }
}
