package com.signalcollect.implementations.manager

import akka.actor.Actor
import akka.actor.Actor._

import com.signalcollect.api.factory._
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration.DistributedConfiguration
import com.signalcollect.util.Constants

class ZombieManager(masterIp: String) extends Manager with Actor {

  // get master hook
  val master = remote.actorFor(Constants.MASTER_MANAGER_SERVICE_NAME, masterIp, Constants.MANAGER_SERVICE_PORT)
  
  // ask for config
  master ! ConfigRequest

  var config: DistributedConfiguration = _

  def receive = {

    // get configuration
    case ConfigResponse(c) =>
      config = c
      createWorkers

  }

  def shutdown = self.stop

  def createWorkers {
    
    // from config, create instance but, can ignore worker factory
    val workerFactory = worker.AkkaRemoteWorker
    
    // TODO, merge remote worker config + workerConfigurations array + change factory of worker config

  }
}