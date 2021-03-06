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

import com.signalcollect.interfaces._
import com.mongodb.casbah.Imports._
import com.signalcollect.implementations.serialization._

/**
 * Stores Vertex IDs in a MongoDB collection
 */
class MongoDBVertexIdSet(vertexStore: Storage) extends VertexIdSet with DefaultSerializer {

  protected var toHandle = vertexSetFactory

  protected def vertexSetFactory = MongoConnection()("todo")(RandomString("", 16))

  def add(vertexId: Any): Unit = {
    toHandle += MongoDBObject("k" -> write(vertexId))
  }

  def remove(vertexId: Any): Unit = {
    toHandle.remove(MongoDBObject("k" -> write(vertexId)))
  }

  def clear {
    toHandle = vertexSetFactory
  }

  def isEmpty(): Boolean = {
    toHandle.isEmpty
  }

  def size: Int = toHandle.size

  def foreach[U](f: (Any) => U, removeAfterProcessing: Boolean) = {
    toHandle.foreach { s =>
      {
        f(vertexStore.vertices.get(read((s.getAs[Array[Byte]]("k")).get)))
        if(removeAfterProcessing) {
        	toHandle.remove(s)          
        }
      }
    }
  }

  def cleanUp = toHandle.dropCollection()
}

trait MongoDBToDoList extends DefaultStorage {
  override protected def vertexSetFactory: VertexIdSet = new MongoDBVertexIdSet(this)
}