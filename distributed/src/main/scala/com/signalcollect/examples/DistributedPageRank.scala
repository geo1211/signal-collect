package com.signalcollect.examples

import com.signalcollect.api._
import com.signalcollect.configuration._
import com.signalcollect.interfaces._

object DistributedPageRank {

  val optionalCg = new DistributedComputeGraphBuilder().withNumberOfNodes(1).withNumberOfWorkers(4).build

  var cg: ComputeGraph = _

  def main(args: Array[String]) {

    if (optionalCg.isDefined) {
      cg = optionalCg.get

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
  }

}