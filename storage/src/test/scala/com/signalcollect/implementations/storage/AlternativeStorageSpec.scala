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
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import com.signalcollect.interfaces._
import com.signalcollect.implementations.messaging.DefaultMessageBus
import com.signalcollect.examples.Page
import java.io._
import com.mongodb.casbah.Imports._
import org.apache.log4j.helpers.LogLog

@RunWith(classOf[JUnitRunner])
class AlternativeStorageSpec extends SpecificationWithJUnit {
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
      val vertexList = List(new Page(0, 0.1), new Page(1, 0.1), new Page(2, 0.1))
      class MongoDBStorage extends DefaultStorage with MongoDB
      val mongoStore = new MongoDBStorage
      vertexList.foreach(v => mongoStore.vertices.put(v))

      "Hold all inserted Vertices" in {
        vertexList.size === mongoStore.vertices.size
      }

      "add all added vertices to the toSignal list" in {
        mongoStore.toSignal.size must_== vertexList.size
      }

      "add all added vertices to the toCollect list" in {
        mongoStore.toCollect.size must_== vertexList.size
      }

      "retreive an added vertex" in {
        mongoStore.vertices.get(1).hashCode === vertexList(1).hashCode
      }

      "reflect changes" in {
        val v1_old: Vertex[_, _] = mongoStore.vertices.get(1)
        val v1_changed = new Page(1, 0.4)
        mongoStore.vertices.updateStateOfVertex(v1_changed)
        val v1_new = mongoStore.vertices.get(1)
        if (v1_old != v1_changed) {
          v1_new.state must_!= v1_old.state //Entries returned differ
        }
        v1_new.state === v1_changed.state
        mongoStore.vertices.size === 3l //old entry is replaced
      }
      
      "clean up after execution" in {
    	  mongoStore.cleanUp
    	  1===1
      }
    }
  }

  "OrientDB" should {
    val currentDir = new java.io.File(".")
    if (hasReadAndWritePermission(currentDir.getCanonicalPath)) {
      val vertexList = List(new Page(0, 0.1), new Page(1, 0.1), new Page(2, 0.1))
      class OrientDB extends DefaultStorage with Orient
      val orientStore = new OrientDB
      vertexList.foreach(v => orientStore.vertices.put(v))

      "Hold all inserted Vertices" in {
        vertexList.size === orientStore.vertices.size
      }

      "add all added vertices to the toSignal list" in {
        orientStore.toSignal.size must_== vertexList.size
      }

      "add all added vertices to the toCollect list" in {
        orientStore.toCollect.size must_== vertexList.size
      }

      "retreive an added vertex" in {
        orientStore.vertices.get(1).hashCode === vertexList(1).hashCode
      }

      "reflect Changes" in {
        val v1_old: Vertex[_, _] = orientStore.vertices.get(1)
        var v1_changed = new Page(1, 0.9)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.8)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.7)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.6)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.5)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.4)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        val v1_new = orientStore.vertices.get(1)
        if (v1_old != v1_changed) {
          v1_new.state must_!= v1_old.state //Entries returned differ
        }
        v1_new.state === v1_changed.state
        orientStore.vertices.size === 3l //old entry is replaced
      }

      "delete a vertex" in {
        orientStore.vertices.remove(1)
        orientStore.vertices.size === 2l //old entry is replaced
      }
      
      "clean up after execution" in {
        orientStore.cleanUp
        1===1
      }
    } else { //No permission in current folder
      "fail gracefully because no write permissions for temp folder exist" in {
        1 === 1
      }
    }
  }
}