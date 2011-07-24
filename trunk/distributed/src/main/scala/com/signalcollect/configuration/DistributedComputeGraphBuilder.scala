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

object DefaultDistributedGraphBuilder extends DistributedGraphBuilder

/**
 * Builder for the creation of a compute graph needs a configuration object for the creation.
 * If the user passes a configuration object but then uses a method of this class, the configuration's object
 * parameter gets overriden ("inserted" in the config object) by the method call's parameter which was passed.
 */
class DistributedGraphBuilder(protected val config: Configuration = new DefaultDistributedConfiguration) extends Serializable {

  def build: ComputeGraph = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => new DistributedBootstrap(config.asInstanceOf[DistributedConfiguration]).boot
    }
  }

  /**
   * Common configuration
   */
  def withNumberOfWorkers(newNumberOfWorkers: Int) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => newRemoteBuilder(numberOfWorkers = newNumberOfWorkers)
    }
  }
  def withLogger(logger: MessageRecipient[LogMessage]) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => newRemoteBuilder(customLogger = Some(logger))
    }
  }
  def withExecutionArchitecture(newExecutionArchitecture: ExecutionArchitecture) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => newRemoteBuilder(executionArchitecture = newExecutionArchitecture)
    }
  }
  def withExecutionConfiguration(newExecutionConfiguration: ExecutionConfiguration) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => newRemoteBuilder(executionConfiguration = newExecutionConfiguration)
    }
  }

  /**
   * Worker configuration
   */
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => newRemoteBuilder(messageBusFactory = newMessageBusFactory)
    }
  }
  def withStorageFactory(newStorageFactory: StorageFactory) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DefaultComputeGraphBuilder for local runs")
      case DistributedExecutionArchitecture => newRemoteBuilder(storageFactory = newStorageFactory)
    }
  }

  /**
   * Distributed configuration
   */
  def withNumberOfNodes(newNumberOfNodes: Int) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Usage of distributed configuration requires Distributed Execution Architecture")
      case DistributedExecutionArchitecture => newRemoteBuilder(numberOfNodes = newNumberOfNodes)
    }
  }
  def withNodesAddress(newNodesAddress: List[String]) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Usage of distributed configuration requires Distributed Execution Architecture")
      case DistributedExecutionArchitecture => newRemoteBuilder(nodesAddress = newNodesAddress)
    }
  }
  def withCoordinatorAddress(newCoordinatorAddress: String) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Usage of distributed configuration requires Distributed Execution Architecture")
      case DistributedExecutionArchitecture => newRemoteBuilder(coordinatorAddress = newCoordinatorAddress)
    }
  }
  def withNodeProvisioning(newNodeProvisioning: NodeProvisioning) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => throw new Exception("Usage of distributed configuration requires Distributed Execution Architecture")
      case DistributedExecutionArchitecture => newRemoteBuilder(nodeProvisioning = newNodeProvisioning)
    }
  }

  /**
   * Builds distributed compute graph
   */
  def newRemoteBuilder(
    numberOfWorkers: Int = config.numberOfWorkers,
    customLogger: Option[MessageRecipient[LogMessage]] = config.customLogger,
    messageBusFactory: MessageBusFactory = config.workerConfiguration.messageBusFactory,
    storageFactory: StorageFactory = config.workerConfiguration.storageFactory,
    executionArchitecture: ExecutionArchitecture = config.executionArchitecture,
    executionConfiguration: ExecutionConfiguration = config.executionConfiguration,
    numberOfNodes: Int = config.asInstanceOf[DistributedConfiguration].numberOfNodes,
    nodesAddress: List[String] = config.asInstanceOf[DistributedConfiguration].nodesAddress,
    coordinatorAddress: String = config.asInstanceOf[DistributedConfiguration].coordinatorAddress,
    nodeProvisioning: NodeProvisioning = config.asInstanceOf[DistributedConfiguration].nodeProvisioning): ComputeGraphBuilder = {
    new ComputeGraphBuilder(
      DefaultDistributedConfiguration(
        numberOfWorkers = numberOfWorkers,
        customLogger = customLogger,
        workerConfiguration = DefaultLocalWorkerConfiguration(
          messageBusFactory = messageBusFactory,
          storageFactory = storageFactory),
        executionConfiguration = executionConfiguration,
        numberOfNodes = numberOfNodes,
        nodesAddress = nodesAddress,
        coordinatorAddress = coordinatorAddress,
        nodeProvisioning = nodeProvisioning))

  }
}
