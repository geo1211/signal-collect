/*
 *  @author Daniel Strebel
 *  @author Philip Stutz
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

package com.signalcollect

import com.signalcollect._
import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.graphproviders._
import com.signalcollect.examples._
import com.signalcollect.implementations.logging.DefaultLogger

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

/**
 * Hint: For information on how to run specs see the specs v.1 website
 * http://code.google.com/p/specs/wiki/RunningSpecs
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends SpecificationWithJUnit {

  val computeGraphFactories: List[Int => Graph] = List(
    (numberOfWorkers: Int) => GraphBuilder.withNumberOfWorkers(numberOfWorkers).build)

  val executionModes = List(OptimizedAsynchronousExecutionMode, SynchronousExecutionMode)

  val testWorkerCounts = List(1, 2, 4, 8 /*, 16, 32, 64, 128*/ )

  def test(graphProviders: List[Int => Graph] = computeGraphFactories, verify: Vertex => Boolean, buildGraph: Graph => Unit = (graph: Graph) => (), numberOfWorkers: Traversable[Int] = testWorkerCounts, signalThreshold: Double = 0, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ExecutionInformation]]()

    for (workers <- numberOfWorkers) {
      for (executionMode <- executionModes) {
        for (graphProvider <- graphProviders) {
          val graph = graphProvider.apply(workers)
          buildGraph(graph)
          val stats = graph.execute(ExecutionConfiguration(executionMode = executionMode, signalThreshold = signalThreshold))
          correct &= graph.aggregate(new AggregationOperation[Boolean] {
            val neutralElement = true
            def aggregate(a: Boolean, b: Boolean): Boolean = a && b
            def extract(v: Vertex): Boolean = verify(v)
          })
          if (!correct) {
            System.err.println("Test failed. Computation stats: " + stats)
          }
          graph.shutdown
        }
      }
    }
    correct
  }

  def buildPageRankGraph(graph: Graph, edgeTuples: Traversable[Tuple2[Int, Int]]): Graph = {
    edgeTuples foreach {
      case (sourceId: Int, targetId: Int) =>
        graph.addVertex(new PageRankVertex(sourceId, 0.85))
        graph.addVertex(new PageRankVertex(targetId, 0.85))
        graph.addEdge(new PageRankEdge(sourceId, targetId))
    }
    graph
  }

  def buildVertexColoringGraph(numColors: Int, graph: Graph, edgeTuples: Traversable[Tuple2[Int, Int]]): Graph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        graph.addVertex(new VerifiedColoredVertex(sourceId, numColors))
        graph.addVertex(new VerifiedColoredVertex(targetId, numColors))
        graph.addEdge(new StateForwarderEdge(sourceId, targetId))
    }
    graph
  }

  def buildSsspGraph(pathSourceId: Any, graph: Graph, edgeTuples: Traversable[Tuple2[Int, Int]]): Graph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        if (sourceId.equals(pathSourceId)) {
          graph.addVertex(new Location(sourceId, Some(0)))
        } else {
          graph.addVertex(new Location(sourceId, None))
        }
        if (targetId.equals(pathSourceId)) {
          graph.addVertex(new Location(targetId, Some(0)))
        } else {
          graph.addVertex(new Location(targetId, None))
        }
        graph.addEdge(new Path(sourceId, targetId))
    }
    graph
  }

  "PageRank algorithm" should {
    "deliver correct results on a 5-cycle graph" in {
      val fiveCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 4), (4, 0))
      def pageRankFiveCycleVerifier(v: Vertex): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = 1.0
        val correct = (state - expectedState).abs < 0.00001
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = pageRankFiveCycleVerifier, buildGraph = buildPageRankGraph(_, fiveCycleEdges)) must_== true
    }

    "deliver correct results on a 5-star graph" in {
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
      test(verify = pageRankFiveStarVerifier, buildGraph = buildPageRankGraph(_, fiveStarEdges)) must_== true
    }

    "deliver correct results on a 2*2 symmetric grid" in {
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
      test(verify = pageRankTwoOnTwoGridVerifier, buildGraph = buildPageRankGraph(_, symmetricTwoOnTwoGridEdges)) must_== true
    }
  }

  def vertexColoringVerifier(v: Vertex): Boolean = {
    v match {
      case v: VerifiedColoredVertex =>
        val verified = !v.publicMostRecentSignals.iterator.contains(v.state)
        if (!verified) {
          println("Vertex Coloring: " + v + " has the same color as one of its neighbors.\n" +
            "Most recent signals received: " + v.publicMostRecentSignals + "\n" +
            "Score signal: " + v.scoreSignal)
        }
        verified
      case other =>
        println("Vertex " + other + " is not of type VerifiedColoredVertex"); false
    }
  }

  "VertexColoring algorithm" should {
    "deliver correct results on a symmetric 4-cycle" in {
      val symmetricFourCycleEdges = List((0, 1), (1, 0), (1, 2), (2, 1), (2, 3), (3, 2), (3, 0), (0, 3))
      test(verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricFourCycleEdges)) must_== true
    }

    "deliver correct results on a symmetric 5-star" in {
      val symmetricFiveStarEdges = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      test(verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricFiveStarEdges)) must_== true
    }
    "deliver correct results on a 2*2 symmetric grid" in {
      val symmetricTwoOnTwoGridEdges = new Grid(2, 2)
      test(verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricTwoOnTwoGridEdges)) must_== true
    }
  }

  "SSSP algorithm" should {
    "deliver correct results on a symmetric 4-cycle" in {
      val symmetricFourCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 0))
      def ssspSymmetricsFourCycleVerifier(v: Vertex): Boolean = {
        val state = v.state.asInstanceOf[Option[Int]].get
        val expectedState = v.id
        val correct = state == expectedState
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = ssspSymmetricsFourCycleVerifier, buildGraph = buildSsspGraph(0, _, symmetricFourCycleEdges)) must_== true
    }

    "deliver correct results on a symmetric 5-star" in {
      val symmetricFiveStarEdges = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      def ssspSymmetricFiveStarVerifier(v: Vertex): Boolean = {
        val state = v.state.asInstanceOf[Option[Int]].get
        val expectedState = if (v.id == 4) 0 else 1
        val correct = state == expectedState
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = ssspSymmetricFiveStarVerifier, buildGraph = buildSsspGraph(4, _, symmetricFiveStarEdges)) must_== true
    }

  }
}

class VerifiedColoredVertex(id: Int, numColors: Int) extends ColoredVertex(id, numColors, 0, false) {
  // only necessary to allow access to vertex internals
  def publicMostRecentSignals: Iterable[Int] = mostRecentSignalMap.values.asInstanceOf[Iterable[Int]]
}