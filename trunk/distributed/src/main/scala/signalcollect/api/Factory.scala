package signalcollect.api

import signalcollect.configuration._
import signalcollect.interfaces._
import signalcollect.implementations.storage._
import signalcollect.implementations.worker._
import signalcollect.implementations.messaging._
import signalcollect.implementations.coordinator.WorkerApi

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.dispatch.Dispatchers
import akka.actor.TypedActor

import signalcollect.util.Constants

object Factory {

  object Storage {

    object InMemory extends StorageFactory {
      def createInstance(messageBus: MessageBus[Any]): Storage = new DefaultStorage(messageBus)
    }
  }

  object MessageBus {

    object AkkaBus extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new AkkaMessageBus[Any](numberOfWorkers, mapper)
    }

  }

  object Worker {
    /**
     * Creating akka workers and returning actor refs for the distributed case
     */
    object AkkaRemote extends AkkaWorkerFactory {
      def createInstance(workerId: Int,
        config: Configuration,
        coordinator: WorkerApi,
        mapper: VertexToWorkerMapper): ActorRef = {

        config.executionArchitecture match {

          case LocalExecutionArchitecture => throw new Exception("Akka remote workers should only be used in the Distributed case.")

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

