/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
 *  
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.evaluation.util

import com.signalcollect.evaluation.algorithms._
import scala.math._
import scala.util.Random
import com.signalcollect._
import scala.collection.mutable.HashSet

/**
 * Utility to create synthetic pages
 * 
 */
class PageFactory(params: GraphParameters) {
  val r = new Random(0)

  /**
   * Creates a page rank page according to the specified graph parameters
   * 
   * @pre id must be within valid i.e. smaller than the graph size
   * @return a page rank page with the specified id and links to other pages according to the distribution determined by the graph parameters
   */
  def getPageForId(id: Int): Vertex = {
    params match {
      case LogNormalParameters(sigma, mu, size) => {
        val page = new MemoryEfficientPage(id)
        val links =  getLogNormalTargetIdArray(id, sigma, mu, size)
        page.setTargetIdArray(links.toArray)
        page
      }
      case _ => null
    }
  }
  
  def getLocationForId(id: Int): Vertex = {
    params match {
      case LogNormalParameters(sigma, mu, size) => {
        val page = new MemoryEfficientLocation(id)
        val links =  getLogNormalTargetIdArray(id, sigma, mu, size)
        page.setTargetIdArray(links.toArray)
        page
      }
      case _ => null
    }
  }
  
  protected def getLogNormalTargetIdArray(id: Int, sigma: Double = 1, mu: Double = 3, graphSize: Int): Array[Int] = {
    val outDegree: Int = exp(mu + sigma * (r.nextGaussian)).round.toInt //log-normal
    val links = new HashSet[Int]
        while (links.size < outDegree) {
          val linkId = ((r.nextDouble * (graphSize - 1))).round.toInt
          if (id != linkId) {
            links.add(linkId)
          }
        }
    links.toArray
  }
  

}