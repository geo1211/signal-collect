package com.signalcollect.implementations.graph

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.signalcollect._

@RunWith(classOf[JUnitRunner])
class GraphModificationSpec extends SpecificationWithJUnit {

  "GraphEditor" should {
    val g = GraphBuilder.build
    g.addVertex(new GraphModificationVertex(0, 1))
    g.addVertex(new GraphModificationVertex(1, 1))
    g.addVertex(new GraphModificationVertex(2, 1))

    g.execute

    "remove all vertices that satisfy a condition" in {
      g.removeVertices(v => (v.asInstanceOf[GraphModificationVertex].id % 2 == 0))
      g.aggregate(new CountVertices[GraphModificationVertex]) === 1
    }
  }
}

class GraphModificationVertex(id: Int, state: Int) extends DataGraphVertex(id, state) {
  type Signal = Int
  def collect(oldState: State, mostRecentSignals: Iterable[Int]): Int = {
    1
  }
}