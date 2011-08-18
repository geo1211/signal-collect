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
import scala.collection.mutable.HashMap

object DefaultDistributedComputeGraphBuilder extends DistributedComputeGraphBuilder

/**
 * Builder for the creation of a compute graph needs a configuration object for the creation.
 * If the user passes a configuration object but then uses a method of this class, the configuration's object
 * parameter gets overriden ("inserted" in the config object) by the method call's parameter which was passed.
 */
class DistributedComputeGraphBuilder(protected val config: DefaultDistributedConfiguration = new DefaultDistributedConfiguration) extends Serializable {

  /**
   * Optional since zombie machines don't get a reference to the compute graph
   * TODO: for the distributed graph loading maybe that is a feature to add
   */
  def build: Option[ComputeGraph] = new DistributedBootstrap(config).bootOption

  /**
   * Common configuration
   */
  def withNumberOfWorkers(newNumberOfWorkers: Int) = newRemoteBuilder(numberOfWorkers = newNumberOfWorkers)
  
  def withLoggingLevel(newLoggingLevel: Int) = newRemoteBuilder(loggingLevel = newLoggingLevel)

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
  def withNumberOfMachines(newNumberOfMachines: Int) = newRemoteBuilder(numberOfMachines = newNumberOfMachines)

  def withMachinesAddress(newMachinesAddress: List[String]) = newRemoteBuilder(machinesAddress = newMachinesAddress)

  def withLeaderAddress(newLeaderAddress: String) = newRemoteBuilder(leaderAddress = newLeaderAddress)

  def withNodeProvisioningFactory(newProvisionFactory: ProvisionFactory) = newRemoteBuilder(provisionFactory = newProvisionFactory)

  /**
   * Builds distributed compute graph
   */
  def newRemoteBuilder(
    numberOfWorkers: Int = config.numberOfWorkers,
    loggingLevel: Int = config.loggingLevel,
    customLogger: Option[MessageRecipient[LogMessage]] = config.customLogger,
    workerFactory: WorkerFactory = config.workerConfiguration.workerFactory,
    messageBusFactory: MessageBusFactory = config.workerConfiguration.messageBusFactory,
    storageFactory: StorageFactory = config.workerConfiguration.storageFactory,
    executionConfiguration: ExecutionConfiguration = config.executionConfiguration,
    numberOfMachines: Int = config.asInstanceOf[DistributedConfiguration].numberOfMachines,
    machinesAddress: List[String] = config.asInstanceOf[DistributedConfiguration].machinesAddress,
    leaderAddress: String = config.asInstanceOf[DistributedConfiguration].leaderAddress,
    provisionFactory: ProvisionFactory = config.asInstanceOf[DistributedConfiguration].provisionFactory,
    workerConfigurations: HashMap[Int, RemoteWorkerConfiguration] = config.asInstanceOf[DistributedConfiguration].workerConfigurations): DistributedComputeGraphBuilder = {
    new DistributedComputeGraphBuilder(
      DefaultDistributedConfiguration(
        numberOfWorkers = numberOfWorkers,
        loggingLevel = loggingLevel,
        customLogger = customLogger,
        executionConfiguration = executionConfiguration,
        workerConfiguration = DefaultRemoteWorkerReferenceConfiguration(
          workerFactory = workerFactory,
          messageBusFactory = messageBusFactory,
          storageFactory = storageFactory),
        numberOfMachines = numberOfMachines,
        machinesAddress = machinesAddress,
        leaderAddress = leaderAddress,
        provisionFactory = provisionFactory,
        workerConfigurations = workerConfigurations))

  }
}
