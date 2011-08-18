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
import com.signalcollect.interfaces.Manager._

import akka.actor.Actor._
import akka.actor.ActorRef

import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.jar._

import com.hazelcast.core._

import scala.collection.JavaConversions._
import scala.util.Random

/** IDENTIFIER FOR MACHINE TYPE USED AT THE BOOTSTRAP */
sealed trait MachineType
case object ZombieType extends MachineType
case object LeaderType extends MachineType

/**
 * Little helper class with common functions executed at the distributed bootstrap
 */
trait BootstrapHelper extends RemoteSendUtils {

  // the machine's local IP
  def localIp = java.net.InetAddress.getLocalHost.getHostAddress

  /**
   * Real worker local instantiation
   *
   * @param: coordinatorForwarder is the reference to the coordinator staying at the leader
   */
  def createLocalWorkers(coordinatorForwarder: ActorRef, config: DistributedConfiguration) {

    val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)

    // get only those workers that should be instantiated at this machine 
    val workers = config.workerConfigurations.filter(x => x._2.ipAddress.equals(localIp))

    // start the workers
    for (idConfig <- workers) {

      val workerId = idConfig._1

      val workerConfig = idConfig._2.asInstanceOf[RemoteWorkerConfiguration]

      val workerFactory = workerConfig.workerFactory

      // create the worker with the retrieved configuration (ip,port), coordinator reference, and mapper
      workerFactory.createInstance(workerId, workerConfig, config.numberOfWorkers, coordinatorForwarder, mapper, config.loggingLevel)

      val worker = remote.actorFor(workerConfig.serviceName, workerConfig.ipAddress, Constants.REMOTE_SERVER_PORT)

      checkAlive(worker)

    } // end for each worker

  }

  /**
   * Wait for a certain task to complete at the zombie side. Can be either for "alive" or for "ready"
   */
  def waitZombie(leaderManager: ActorRef, checkType: Any) {

    var isCompleted = false

    // waits for zombie availability
    while (!isCompleted) {

      val result = leaderManager !! checkType

      result match {
        case Some(reply) => isCompleted = reply.asInstanceOf[Boolean] // handle reply
        case None => sys.error("timeout waiting for reply")
      }

      Thread.sleep(100)

    }
  }

  /**
   * Part of the leader election algorithm
   */
  def getLeaderIpFromBootManager(bootManager: ActorRef): String = {

    var decided = false

    var leaderIp = ""

    while (!decided) {

      val result = bootManager !! RequestLeaderIp

      result match {
        case Some(reply) =>
          leaderIp = reply.asInstanceOf[String] // handle reply

          if (!leaderIp.equals("undecided"))
            decided = true

        case None => sys.error("timeout waiting for reply")
      }

      Thread.sleep(100)

    }

    leaderIp

  }

  /**
   * Hazelcast sequence of commands for starting the "cluster" and finding machines on it
   *
   * @return a list with all the IP address of the available machines
   */
  def startHazelcastGetAddresses(numberOfMachines: Int): List[String] = {

    var hazelcastSuccess = false

    // hazelcast config
    val hcConfigXml = new com.hazelcast.config.FileSystemXmlConfig(getHazelcastConfigFile) //-Dhazelcast.logging.type=none

    // hazelcast cluster
    val cluster = Hazelcast.init(hcConfigXml).getCluster

    while (!hazelcastSuccess) {
      // variable for timeout
      var wait = 0

      var shouldRestart = false

      // wait until all machines have joined
      println("Waiting HAZELCAST")
      while (cluster.getMembers().size() != numberOfMachines || !shouldRestart) {
        Thread.sleep(500)
        wait = wait + 1

        if (wait == 240) // after 2 minutes
        {
          sys.error("After " + wait / 2 + " seconds, not all machines came online. Abort.")
          System.exit(-1)
        } // after 30 seconds and if it failed to get cluster reference
        else if (wait == 60 && (cluster.getMembers().size() == 1)) {
          shouldRestart = true
        }
      }

      if (!shouldRestart)
        hazelcastSuccess = true
      else
        Hazelcast.restart()

    }

    val membersSet = cluster.getMembers

    var machinesAddress: List[String] = List()

    for (member <- membersSet)
      machinesAddress = member.getInetAddress().getHostAddress() :: machinesAddress

    machinesAddress
  }

  /**
   * Writes hazelcast.xml file from jar to the filesystem in order to load it inside the bootstrap
   */
  def getHazelcastConfigFile: File = {

    var file: File = null

    val classpath = System.getProperty("java.class.path")

    val jarFile = new JarFile(classpath)

    val enum = jarFile.entries()

    var found = false

    while (enum.hasMoreElements() && !found)
      found = process(enum.nextElement())

    def process(obj: Object): Boolean = {
      val entry = obj.asInstanceOf[JarEntry]
      val name = entry.getName()

      if (name.contains("hazelcast.xml")) {

        val in = jarFile.getInputStream(entry)

        val dir = new java.io.File(System.getProperty("user.home") + "/tmp")

        if (!dir.exists)
          dir.mkdirs

        file = new java.io.File(System.getProperty("user.home") + "/tmp/" + "hazelcast.xml")

        val out = new FileOutputStream(file)
        val buf = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
        val len = buf.length

        out.write(buf, 0, len)
        out.close

        in.close

        true

      } else
        false

    }

    file

  }

  /**
   * Function to shutdown hazelcast
   */
  def shutdownHazelcast = Hazelcast.shutdownAll()

}