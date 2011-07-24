package com.signalcollect

import org.clapper.argot._
import org.clapper.argot.ArgotConverters._

import akka.actor.Actor
import akka.actor.Actor._

import com.signalcollect.util.Constants
import com.signalcollect.implementations.manager.ZombieManager

import java.net.InetAddress

object Zombie {

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    val masterIp = args(0)

    // start local manager
    remote.start(InetAddress.getLocalHost.getHostAddress, Constants.MANAGER_SERVICE_PORT)
    remote.register(Constants.ZOMBIE_MANAGER_SERVICE_NAME, actorOf(new ZombieManager(masterIp)))

  }

}