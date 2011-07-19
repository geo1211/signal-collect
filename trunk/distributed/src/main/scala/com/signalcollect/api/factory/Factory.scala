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

import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.implementations.storage._
import com.signalcollect.implementations.worker._
import com.signalcollect.implementations.messaging._
import com.signalcollect.implementations.coordinator.WorkerApi

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.dispatch.Dispatchers
import akka.actor.TypedActor

import com.signalcollect.util.Constants

package factory {

  /*package messageBus {

    object AkkaBus extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new AkkaMessageBus[Any](numberOfWorkers, mapper)
    }

  }*/

  package worker {
    /**
     * Creating akka workers and returning actor refs for the distributed case
     */
    object AkkaRemote extends AkkaWorkerFactory {
      def createInstance(workerId: Int,
        config: Configuration,
        coordinator: WorkerApi,
        mapper: VertexToWorkerMapper): ActorRef = {

        config.executionArchitecture match {

          case LocalExecutionArchitecture => throw new Exception("Akka remote workers can only be used in the Distributed case.")

          /**
           *  In case its distributed, worker instantiation is different.
           *  The factory will be just getting the hook to the remote worker.
           *  Creation of workers in a distributed case happen via the Distributed Bootstrap
           */
          case DistributedExecutionArchitecture =>
            // get remote worker configuration
            val workerConf = config.asInstanceOf[DistributedConfiguration].workerConfigurations.get(workerId).asInstanceOf[RemoteWorkerConfiguration]

            // get the hook for the remote actor as a actor ref
            Actor.remote.actorFor(Constants.WORKER_SERVICE_NAME + "" + workerId, workerConf.ipAddress, workerConf.port)
        }

      }
    }
  }

}

