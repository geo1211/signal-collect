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

import java.util.Set
import com.signalcollect.interfaces._
import java.util.HashMap

class SerializingInMemoryStorage(storage: Storage) extends VertexStore {
  protected var vertexMap = new HashMap[Any, Array[Byte]]()
  protected val serializer = storage.serializer

  def get(id: Any): Vertex = {
    serializer.read(vertexMap.get(id))
  }

  def put(vertex: Vertex): Boolean = {
    if (!vertexMap.containsKey(vertex.id)) {
      vertexMap.put(vertex.id, serializer.write(vertex))
      storage.toCollect.addVertex(vertex.id)
      storage.toSignal.add(vertex.id)
      true
    } else
      false
  }
  def remove(id: Any) = {
    vertexMap.remove(id)
    storage.toCollect.remove(id)
    storage.toSignal.remove(id)
  }

  def updateStateOfVertex(vertex: Vertex) = {
    vertexMap.put(vertex.id, serializer.write(vertex))
  }

  def foreach[U](f: (Vertex) => U) {
    val it = vertexMap.values.iterator
    while (it.hasNext) {
      val vertex: Vertex = serializer.read(it.next)
      f(vertex)
      updateStateOfVertex(vertex)
    }
  }

  def size: Long = vertexMap.size
  
  def cleanUp = vertexMap.clear
}

/**
 * Allows mixing in MongoDB in the DefaultStorage Implementation to change its storage back end.
 */
trait Serializing extends DefaultStorage {
  override protected def vertexStoreFactory = new SerializingInMemoryStorage(this)
}