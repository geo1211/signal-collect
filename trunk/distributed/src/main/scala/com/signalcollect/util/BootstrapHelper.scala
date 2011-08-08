package com.signalcollect.util

import com.signalcollect.configuration._
import com.signalcollect.implementations.messaging.DefaultVertexToWorkerMapper

import akka.actor.Actor._
import akka.actor.ActorRef

import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress

sealed trait MachineType
case object ZombieType extends MachineType
case object LeaderType extends MachineType

trait BootstrapHelper extends RemoteSendUtils {

  def createLocalWorkers(coordinatorForwarder: ActorRef, config: DistributedConfiguration) {

    val localIp = InetAddress.getLocalHost.getHostAddress

    val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

    // get only those workers that should be instantiated at this node 
    val workers = config.workerConfigurations.filter(x => x._2.ipAddress.equals(localIp))

    // start the workers
    for (idConfig <- workers) {

      val workerId = idConfig._1

      val workerConfig = idConfig._2

      val workerFactory = workerConfig.workerFactory

      // create the worker with the retrieved configuration (ip,port), coordinator reference, and mapper
      workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, coordinatorForwarder, mapper)

      val worker = remote.actorFor(workerConfig.asInstanceOf[RemoteWorkerConfiguration].serviceName, workerConfig.asInstanceOf[RemoteWorkerConfiguration].ipAddress, Constants.REMOTE_SERVER_PORT)

      checkAlive(worker)

    } // end for each worker

  }

  def waitZombie(leaderManager: ActorRef, checkType: Any) {

    var isCompleted = false

    // waits for zombie availability
    // TODO: ADD TIMEOUT
    while (!isCompleted) {

      val result = leaderManager !! checkType

      result match {
        case Some(reply) => isCompleted = reply.asInstanceOf[Boolean] // handle reply
        case None        => sys.error("timeout waiting for reply")
      }

      Thread.sleep(100)

    }

  }

  def getHazelcastConfigFile: File = {

    var folder = ""

    if (System.getProperty("os.name").startsWith("Windows"))
      folder = "c:/temp/"
    else
      folder = "/tmp/"

    val file = new java.io.File(folder + "hazelcast.xml")

    if (!file.exists()) {
      val is = this.getClass().getResourceAsStream("hazelcast.xml")
      val out = new FileOutputStream(file)
      val buf = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
      val len = buf.length

      out.write(buf, 0, len)
      out.close

      is.close
    }

    file

  }

}