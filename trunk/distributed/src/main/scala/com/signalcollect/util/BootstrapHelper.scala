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

package com.signalcollect.util

import com.signalcollect.configuration._
import com.signalcollect.implementations.messaging.DefaultVertexToWorkerMapper

import akka.actor.Actor._
import akka.actor.ActorRef

import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress

import com.hazelcast.core._

import scala.util.Random

sealed trait MachineType
case object ZombieType extends MachineType
case object LeaderType extends MachineType

trait BootstrapHelper extends RemoteSendUtils {

  // the machine's local IP
  def localIp = java.net.InetAddress.getLocalHost.getHostAddress

  def createLocalWorkers(coordinatorForwarder: ActorRef, config: DistributedConfiguration) {

    val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

    // get only those workers that should be instantiated at this machine 
    val workers = config.workerConfigurations.filter(x => x._2.ipAddress.equals(localIp))

    // start the workers
    for (idConfig <- workers) {

      val workerId = idConfig._1

      val workerConfig = idConfig._2.asInstanceOf[RemoteWorkerConfiguration]

      val workerFactory = workerConfig.workerFactory

      //println("ID = " + workerId + "@" + workerConfig.ipAddress + " - " + workerConfig.serviceName)

      // create the worker with the retrieved configuration (ip,port), coordinator reference, and mapper
      workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, coordinatorForwarder, mapper)

      val worker = remote.actorFor(workerConfig.serviceName, workerConfig.ipAddress, Constants.REMOTE_SERVER_PORT)

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
        case None => sys.error("timeout waiting for reply")
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