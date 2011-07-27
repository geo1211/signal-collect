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

package com.signalcollect.api

import com.signalcollect.interfaces._
import com.signalcollect.implementations.worker._
import com.signalcollect.implementations.messaging._
import com.signalcollect.configuration._
import com.signalcollect.configuration.provisioning._

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

import com.signalcollect.util.Constants

package factory {

  package provision {

    object EqualProvisioning extends ProvisionFactory {
      def createInstance(config: DistributedConfiguration): NodeProvisioning = new EqualNodeProvisioning(config)
    }
  }

  package messageBus {

    object AkkaBus extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new AkkaMessageBus[Any](numberOfWorkers, mapper)
    }

  }

  package worker {

    object AkkaLocal extends AkkaWorkerFactory {
      def createInstance(workerId: Int,
                         workerConfig: WorkerConfiguration,
                         numberOfWorkers: Int,
                         coordinator: Any,
                         mapper: VertexToWorkerMapper): ActorRef = actorOf(new AkkaWorker(workerId, workerConfig, numberOfWorkers, coordinator, mapper))
    }

    /**
     * Worker real creation
     *
     * This is used by zombies to start remote workers
     */
    object AkkaRemoteWorker extends WorkerFactory {
      def createInstance(workerId: Int,
                         workerConfig: WorkerConfiguration,
                         numberOfWorkers: Int,
                         coordinator: Any,
                         mapper: VertexToWorkerMapper): Any = {

        // TODO: test if coordinator is an actor class?

        // info coming from config
        remote.start(workerConfig.asInstanceOf[RemoteWorkerConfiguration].ipAddress, workerConfig.asInstanceOf[RemoteWorkerConfiguration].port)

        // register worker
        remote.register(Constants.WORKER_SERVICE_NAME + "" + workerId, actorOf(new AkkaWorker(workerId, workerConfig, numberOfWorkers, coordinator, mapper)))

      }

    }

    /**
     * Creating akka worker references for the distributed case.
     * The factory just gets the hook to the remote worker.
     *
     * This factory should be used in the coordinator side
     *
     */
    object AkkaRemoteReference extends AkkaWorkerFactory {
      def createInstance(workerId: Int,
                         workerConfig: WorkerConfiguration,
                         numberOfWorkers: Int,
                         coordinator: Any,
                         mapper: VertexToWorkerMapper): ActorRef = {

        /**
         *  The Real creation of workers in a distributed case happen via the Distributed Bootstrap using [AkkaRemoteWorker] factory
         */

        // get the hook for the remote actor as a actor ref
        Actor.remote.actorFor(Constants.WORKER_SERVICE_NAME + "" + workerId, workerConfig.asInstanceOf[RemoteWorkerConfiguration].ipAddress, workerConfig.asInstanceOf[RemoteWorkerConfiguration].port)

      }
    }
  }

}

