/*
 *  @author Daniel Strebel
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
 */

package com.signalcollect.implementations.storage

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import com.signalcollect._
import com.signalcollect.interfaces._
import com.signalcollect.implementations.messaging.DefaultMessageBus
import java.io._
import com.mongodb.casbah.Imports._
import org.apache.log4j.helpers.LogLog
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class AlternativeStorageSpec extends SpecificationWithJUnit with Mockito {
  LogLog.setQuietMode(true)

  /**
   * Check for read/write permission on temporary folder
   */
  def hasReadAndWritePermission(path: String): Boolean = {
    val tempFolder = new File(path)
    tempFolder.canWrite && tempFolder.canRead
  }

  "MongoDB" should {

    var mongoIsRunning = true
    var mongoStore = MongoConnection()("sc")("ModuleTest") //connect to localhost at port 27017

    try {
      mongoStore.size
    } catch {
      case _ => {
        println("Mongo isn't running")
        mongoIsRunning = false
      }
    }

    if (!mongoIsRunning) {
      "fail gracefully if MongoDB is not present" in {
        1 === 1
      }
    } else {

      val dummyVertices = ListBuffer[Vertex]()
      for (i <- 0 until 3) {
        dummyVertices += new DummyVertex(i, i)
      }

      class MongoDBStorage extends DefaultStorage with MongoDB
      val mongoStore = new MongoDBStorage
      dummyVertices.foreach(v => mongoStore.vertices.put(v))

      "Hold all inserted Vertices" in {
        dummyVertices.size === mongoStore.vertices.size
      }

      "retreive an added vertex" in {
        mongoStore.vertices.get(0).hashCode === dummyVertices(0).hashCode
        mongoStore.vertices.get(1).hashCode === dummyVertices(1).hashCode
        mongoStore.vertices.get(2).hashCode === dummyVertices(2).hashCode
      }

      "reflect changes" in {
        
        // Save the current entry for later comparison
        val v1_old: Vertex = mongoStore.vertices.get(1)

        // Enter modifications with the same vetex-ID
        mongoStore.vertices.updateStateOfVertex(new DummyVertex(1, 7))
        mongoStore.vertices.updateStateOfVertex(new DummyVertex(1, 6))
        mongoStore.vertices.updateStateOfVertex(new DummyVertex(1, 5))
        mongoStore.vertices.updateStateOfVertex(new DummyVertex(1, 4))

        // Retrieve current vertex for the ID
        val v1_new = mongoStore.vertices.get(1)

        //Tests
        v1_new.state === 4 //state equals state of the last update
        v1_old.state !== v1_new.state
        mongoStore.vertices.size === 3l //old entry is replaced
      }
      "clean up after execution" in {
        mongoStore.cleanUp
        1 === 1
      }
    }
  }

  "OrientDB" should {
    val currentDir = new java.io.File(".")
    if (hasReadAndWritePermission(currentDir.getCanonicalPath)) {

      val dummyVertices = ListBuffer[Vertex]()
      for (i <- 0 until 3) {
        dummyVertices += new DummyVertex(i, i)
      }

      val page1 = mock[Vertex]
      val page2 = mock[Vertex]

      class OrientDB extends DefaultStorage with Orient
      val orientStore = new OrientDB
      dummyVertices.foreach(v => orientStore.vertices.put(v))

      "Hold all inserted Vertices" in {
        dummyVertices.size === orientStore.vertices.size
      }

      "retreive an added vertex" in {
        orientStore.vertices.get(0).hashCode === dummyVertices(0).hashCode
        orientStore.vertices.get(1).hashCode === dummyVertices(1).hashCode
        orientStore.vertices.get(2).hashCode === dummyVertices(2).hashCode

      }

      "reflect Changes" in {
        // Save the current entry for later comparison
        val v1_old: Vertex = orientStore.vertices.get(1)

        // Enter modifications with the same vetex-ID
        orientStore.vertices.updateStateOfVertex(new DummyVertex(1, 7))
        orientStore.vertices.updateStateOfVertex(new DummyVertex(1, 6))
        orientStore.vertices.updateStateOfVertex(new DummyVertex(1, 5))
        orientStore.vertices.updateStateOfVertex(new DummyVertex(1, 4))

        // Retrieve current vertex for the ID
        val v1_new = orientStore.vertices.get(1)

        //Tests
        v1_new.state === 4 //state equals state of the last update
        v1_old.state !== v1_new.state
        orientStore.vertices.size === 3l //old entry is replaced
      }

      "delete a vertex" in {
        orientStore.vertices.remove(1)
        orientStore.vertices.size === 2l //old entry is replaced
      }

      "clean up after execution" in {
        orientStore.cleanUp
        1 === 1
      }
    } else { //No permission in current folder
      "fail gracefully because no write permissions for temp folder exist" in {
        1 === 1
      }
    }
  }
}

class DummyVertex(id: Int, value: Int) extends DataGraphVertex(id, value) {

  type Signal = Int

  def collect(oldState: State, mostRecentSignals: Iterable[Int]): Int = {
    id + 1
  }

  override def scoreSignal: Double = {
    print("hello")
    1
  }

}

