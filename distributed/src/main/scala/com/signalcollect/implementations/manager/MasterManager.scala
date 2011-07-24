package com.signalcollect.implementations.manager

import akka.actor.Actor
import com.signalcollect.interfaces.Manager
import com.signalcollect.interfaces.Manager._
import com.signalcollect.configuration.DistributedConfiguration

class MasterManager(config: DistributedConfiguration) extends Manager with Actor {
  
  var nodeCounter = 0
  var workerId = 0
  
  var nodesJoined: List[String] = _

  def receive = {

    // a zombie requested the configuration
    case ConfigRequest(from) =>
      
      // add node joined
      nodesJoined = from :: nodesJoined
      
      // send back configuration
      self.reply(ConfigResponse(config))
      
      // check if nodes joined == nodesAddress from config
      
      

  }

  def shutdown = self.stop

}