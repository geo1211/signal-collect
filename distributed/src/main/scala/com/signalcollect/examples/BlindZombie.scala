package com.signalcollect.examples

import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.interfaces.Manager._

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

class BlindZombie extends Actor {

  def receive = {

    case Start(x) =>
      new DistributedComputeGraphBuilder().withNumberOfMachines(x).build

    case "Stop" =>
      self.stop()

  }

}

object BlindZombie {

  // the machine's local IP
  val localIp = java.net.InetAddress.getLocalHost.getHostAddress

  def main(args: Array[String]) {

    remote.start(localIp, 2552)

    remote.register("blind-zombie", actorOf[BlindZombie])

  }
}