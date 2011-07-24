package com.signalcollect.interfaces

import com.signalcollect.configuration.DistributedConfiguration

trait Manager {

  def shutdown

}

object Manager {
  sealed trait ManagerMessage
  case class ConfigRequest(from: String) extends ManagerMessage  
  case class ConfigResponse(config: DistributedConfiguration) extends ManagerMessage
}