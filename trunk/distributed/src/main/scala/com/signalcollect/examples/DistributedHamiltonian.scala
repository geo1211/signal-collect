package com.signalcollect.examples

import com.signalcollect.api._
import com.signalcollect.configuration._
import com.signalcollect.interfaces._

object DistributedHamiltonian extends App {
  val optionalCg = new DistributedComputeGraphBuilder().withNumberOfNodes(1).build

  var cg: ComputeGraph = _

  if (optionalCg.isDefined) {
    cg = optionalCg.get

    cg.addVertex(new HamiltonianVertex("a", Map(List("a") -> 0)))
    cg.addVertex(new HamiltonianVertex("b", Map(List("b") -> 0)))
    cg.addVertex(new HamiltonianVertex("c", Map(List("c") -> 0)))
    cg.addVertex(new HamiltonianVertex("d", Map(List("d") -> 0)))
    cg.addVertex(new HamiltonianVertex("e", Map(List("e") -> 0)))

    cg.addEdge(new HamiltonianEdge("a", "d", 3)); cg.addEdge(new HamiltonianEdge("d", "a", 3))
    cg.addEdge(new HamiltonianEdge("a", "b", 1)); cg.addEdge(new HamiltonianEdge("b", "a", 1))
    cg.addEdge(new HamiltonianEdge("d", "b", 2)); cg.addEdge(new HamiltonianEdge("b", "d", 2))
    cg.addEdge(new HamiltonianEdge("d", "c", 1)); cg.addEdge(new HamiltonianEdge("c", "d", 1))
    cg.addEdge(new HamiltonianEdge("b", "c", 1)); cg.addEdge(new HamiltonianEdge("c", "b", 1))

    // a problem with isolated vertices is that it is not able to find hamiltonian paths depending on the starting vertex
    cg.addEdge(new HamiltonianEdge("e", "a", 1)); cg.addEdge(new HamiltonianEdge("a", "e", 1))

    val stats = cg.execute
    println(stats)
    cg.foreachVertex(println(_))
    cg.shutdown
  }
}