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

package com.signalcollect.factory

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import com.signalcollect.interfaces.ProvisionFactory
import com.signalcollect.configuration.provisioning.EqualNodeProvisioning
import com.signalcollect.configuration.DistributedConfiguration
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.implementations.messaging.AkkaMessageBus
import com.signalcollect.implementations.messaging.AkkaMessageBusWithRemote
import com.signalcollect.implementations.worker.AkkaWorker
import com.signalcollect.interfaces.AkkaWorkerFactory
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.configuration.provisioning.NodeProvisioning
import com.signalcollect.configuration.RemoteWorkerConfiguration
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.WorkerConfiguration
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.util.Constants

/**
 * Definitions for provisioning factory
 */
package provision {

  /**
   * Equal provisioning of workers per machine
   */
  object EqualProvisioning extends ProvisionFactory {
    def createInstance(config: DistributedConfiguration): NodeProvisioning = new EqualNodeProvisioning(config)
  }
}

package messageBus {

  /**
   * Used by the distributed case when communicating with remote actors
   */
  object AkkaBusRemote extends MessageBusFactory {
    def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new AkkaMessageBusWithRemote[Any](numberOfWorkers, mapper)
  }

  /**
   * Used by the shared memory implementation of AKka workers
   */
  object AkkaBus extends MessageBusFactory {
    def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new AkkaMessageBus[Any](numberOfWorkers, mapper)
  }

}

package worker {

  /**
   * Shared memory implementation of Akka workers
   */
  object AkkaLocal extends AkkaWorkerFactory {
    override def createInstance(workerId: Int,
      workerConfig: WorkerConfiguration,
      numberOfWorkers: Int,
      coordinator: Any,
      mapper: VertexToWorkerMapper,
      loggingLevel: Int): ActorRef = actorOf(new AkkaWorker(workerId, workerConfig, numberOfWorkers, coordinator, mapper, loggingLevel))
  }

  /**
   * Worker real creation
   *
   * This is used to start remote workers
   */
  object AkkaRemoteWorker extends WorkerFactory {
    def createInstance(workerId: Int,
      workerConfig: WorkerConfiguration,
      numberOfWorkers: Int,
      coordinator: Any,
      mapper: VertexToWorkerMapper,
      loggingLevel: Int): Any = {

      // register worker in Akka server registry
      remote.register(Constants.WORKER_NAME + "" + workerId, actorOf(new AkkaWorker(workerId, workerConfig, numberOfWorkers, coordinator, mapper, loggingLevel)))

    }

  }

  /**
   * Creating akka worker references for the distributed case.
   * The factory just gets the hook to the remote worker.
   *
   * This factory is used at the coordinator side
   *
   */
  object AkkaRemoteReference extends AkkaWorkerFactory {
    def createInstance(workerId: Int,
      workerConfig: WorkerConfiguration,
      numberOfWorkers: Int,
      coordinator: Any,
      mapper: VertexToWorkerMapper,
      loggingLevel: Int): ActorRef = {

      // get the hook for the remote actor as an actor ref
      val worker = remote.actorFor(workerConfig.asInstanceOf[RemoteWorkerConfiguration].serviceName, workerConfig.asInstanceOf[RemoteWorkerConfiguration].ipAddress, Constants.REMOTE_SERVER_PORT).start

      worker

    }
  }
}

