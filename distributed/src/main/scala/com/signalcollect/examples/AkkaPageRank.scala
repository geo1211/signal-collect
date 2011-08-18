/*
 *  @author Philip Stutz
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

package com.signalcollect.examples

import com.signalcollect.factory._
import com.signalcollect.implementations.graph.SumOfOutWeights
import com.signalcollect.configuration._

import java.io.{ ByteArrayOutputStream, ByteArrayInputStream, ObjectOutputStream, ObjectInputStream }
import com.signalcollect.interfaces._
import java.util.LinkedList

/** Builds a PageRank compute graph and executes the computation */
object AkkaPageRank extends App {
  val cg = AkkaLocalGraphBuilder.withMessageBusFactory(messageBus.AkkaBus).withWorkerFactory(worker.AkkaLocal).build
  cg.addVertex(new Page(1))
  cg.addVertex(new Page(2))
  cg.addVertex(new Page(3))
  cg.addEdge(new Link(1, 2))
  cg.addEdge(new Link(2, 1))
  cg.addEdge(new Link(2, 3))
  cg.addEdge(new Link(3, 2))
  val stats = cg.execute
  println(stats)
  cg.foreachVertex(println(_))
  cg.shutdown
}
