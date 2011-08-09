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

package com.signalcollect.configuration.provisioning

import scala.collection.mutable.HashMap
import com.signalcollect.util.Constants
import com.signalcollect.configuration._

/**
 *
 * Equal node provisioning will put the same amount of workers in every node.
 * In case in case it's and odd number of workers and nodes, the first node gets one more worker than the others
 * eg. 25 workers in 3 nodes = 9 on the first, 8 on the other 2
 */
class EqualNodeProvisioning(config: DistributedConfiguration) extends NodeProvisioning {

  override def toString = "EqualNodeProvisioning"

  def workersPerNodeNames {

    val nodesAddress = config.nodesAddress
    val numberOfNodes = config.numberOfNodes

    // equal division of workers among all nodes
    val div = config.numberOfWorkers.asInstanceOf[Double] / numberOfNodes.asInstanceOf[Double]

    var workerCounter = 0

    // for each node 
    for (i <- 0 to numberOfNodes - 1) {

      var numWorkersAtNode = 0

      val ip = nodesAddress(i)

      // in case its an odd number of workers and nodes, the first node gets one more worker than the others
      if (i == 0)
        numWorkersAtNode = math.ceil(div).asInstanceOf[Int]
      else
        numWorkersAtNode = math.floor(div).asInstanceOf[Int]

      numWorkersAtNode = numWorkersAtNode + workerCounter

      var names = List[String]()

      for (j <- workerCounter to numWorkersAtNode - 1) {
        // create specific configuration for the worker
        val remoteWorkerConfiguration = DefaultRemoteWorkerConfiguration(ipAddress = ip, serviceName = Constants.WORKER_SERVICE_NAME + "" + j)

        // add the worker configuration to the list
        config.workerConfigurations.put(j, remoteWorkerConfiguration)
      }

      // increment id counter
      workerCounter = workerCounter + numWorkersAtNode

    }

  }

}