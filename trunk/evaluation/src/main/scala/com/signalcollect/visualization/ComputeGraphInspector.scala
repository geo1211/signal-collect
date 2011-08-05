package com.signalcollect.visualization

import com.signalcollect.interfaces._
import java.util.LinkedList
import scala.collection.JavaConversions._
import scala.collection.JavaConversions
import com.signalcollect.configuration.ExecutionConfiguration
import com.signalcollect.configuration.SynchronousExecutionMode

class ComputeGraphInspector(val cg: ComputeGraph) {

  def getNeighbors(v: Vertex): java.lang.Iterable[Vertex] = {
    val neighbors = new LinkedList[Vertex]()
    for (neighborId <- v.getVertexIdsOfNeighbors) {
      val neighbor = cg.forVertexWithId(neighborId, { v: Vertex => v })
      if (neighbor.isDefined) {
        neighbors.add(neighbor.get)
      }
    }
    neighbors
  }

  def getEdges(v: Vertex): java.lang.Iterable[Edge] = {
    val result = new LinkedList[Edge]()
    val edgesOption = v.getOutgoingEdges
    if (edgesOption.isDefined) {
      for (edge <- edgesOption.get) {
        result.add(edge)
      }
    }
    result
  }

  def isInt(s: String): Boolean = {
    try {
      s.toInt
      true
    } catch {
      case someProblem => false
    }
  }

  def searchVertex(vertexId: String): java.lang.Iterable[Vertex] = {
    if (isInt(vertexId)) {
      val vertex = getVertexWithId(vertexId.toInt.asInstanceOf[AnyRef])
      if (vertex != null) {
        List[Vertex](vertex)
      } else {
        List[Vertex]()
      }
    } else {
      cg.customAggregate(List[Vertex](), { (a: List[Vertex], b: List[Vertex]) =>
        a ++ b
      }, { v: Vertex =>
        if (v.id.toString.contains(vertexId))
          List(v)
        else {
          List[Vertex]()
        }
      })
    }
  }

  def getVertexWithId(id: Object): Vertex = {
    val vertexOption = cg.forVertexWithId(id, { v: Vertex => v })
    if (vertexOption.isDefined) {
      vertexOption.get
    } else {
      null
    }
  }

  def executeComputationStep {
    cg.execute(ExecutionConfiguration(executionMode = SynchronousExecutionMode, stepsLimit = Some(1), signalThreshold = 0.0))
  }

}