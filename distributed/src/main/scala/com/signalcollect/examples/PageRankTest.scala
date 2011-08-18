package com.signalcollect.examples

import com.signalcollect._
import com.signalcollect.api._
import com.signalcollect.api.factory._
import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.interfaces.Manager._
import com.signalcollect.examples._
import com.signalcollect.implementations.logging.DefaultLogger

import akka.actor.Actor._

object PageRankTest {

  class Grid(val width: Int, height: Int) extends Traversable[(Int, Int)] {

    def foreach[U](f: ((Int, Int)) => U) = {
      val max = width * height
      for (n <- 1 to max) {
        if (n + width <= max) {
          f(n, n + width)
          f(n + width, n)
        }
        if (n % height != 0) {
          f(n, n + 1)
          f(n + 1, n)
        }
      }
    }
  }

  val computeGraphFactories: List[Int => ComputeGraph] = List(
    (numberOfWorkers: Int) => DefaultDistributedComputeGraphBuilder.withNumberOfMachines(1).withNumberOfWorkers(numberOfWorkers).build.get)

  val executionModes = List(OptimizedAsynchronousExecutionMode, SynchronousExecutionMode)

  val testWorkerCounts = List(2, 4, 8, 16 /*, 32, 64, 128*/ )

  def test(graphProviders: List[Int => ComputeGraph] = computeGraphFactories, verify: Vertex => Boolean, buildGraph: ComputeGraph => Unit = (cg: ComputeGraph) => (), numberOfWorkers: Traversable[Int] = testWorkerCounts, signalThreshold: Double = 0, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ExecutionInformation]]()

    for (workers <- numberOfWorkers) {
      for (executionMode <- executionModes) {
        for (graphProvider <- graphProviders) {

          val cg = graphProvider.apply(workers)

          buildGraph(cg)

          val stats = cg.execute(ExecutionConfiguration(executionMode = executionMode, signalThreshold = signalThreshold))

          correct &= cg.customAggregate(true, (a: Boolean, b: Boolean) => (a && b), verify)
          if (!correct) {
            System.err.println("Test failed. Computation stats: " + stats)
          }

          cg.shutdown

        }
      }
    }
    correct
  }

  def buildPageRankGraph(cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId: Int, targetId: Int) =>
        cg.addVertex(new Page(sourceId, 0.85))
        cg.addVertex(new Page(targetId, 0.85))
        cg.addEdge(new Link(sourceId, targetId))
    }
    cg
  }

  def main(args: Array[String]) {

    val fiveCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 4), (4, 0))
    def pageRankFiveCycleVerifier(v: Vertex): Boolean = {
      val state = v.state.asInstanceOf[Double]
      val expectedState = 1.0
      val correct = (state - expectedState).abs < 0.00001

      //System.out.println("DEBUG = Expected state=" + expectedState + " actual state=" + state)

      if (!correct) {
        System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
      }
      correct
    }
    if (!test(verify = pageRankFiveCycleVerifier, buildGraph = buildPageRankGraph(_, fiveCycleEdges)))
      sys.error("NOT TRUE")

    val fiveStarEdges = List((0, 4), (1, 4), (2, 4), (3, 4))
    def pageRankFiveStarVerifier(v: Vertex): Boolean = {
      val state = v.state.asInstanceOf[Double]
      val expectedState = if (v.id == 4.0) 0.66 else 0.15
      val correct = (state - expectedState).abs < 0.00001
      if (!correct) {
        System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
      }
      correct
    }
    if (!test(verify = pageRankFiveStarVerifier, buildGraph = buildPageRankGraph(_, fiveStarEdges)))
      sys.error("NOT TRUE")

    val symmetricTwoOnTwoGridEdges = new Grid(2, 2)
    def pageRankTwoOnTwoGridVerifier(v: Vertex): Boolean = {
      val state = v.state.asInstanceOf[Double]
      val expectedState = 1.0
      val correct = (state - expectedState).abs < 0.00001
      if (!correct) {
        System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
      }
      correct
    }
    if (!test(verify = pageRankTwoOnTwoGridVerifier, buildGraph = buildPageRankGraph(_, symmetricTwoOnTwoGridEdges)))
      sys.error("NOT TRUE")
  }

  // do here for the others

}