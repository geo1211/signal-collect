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

import com.mongodb.casbah.Imports._
import com.signalcollect._
import com.signalcollect.interfaces._
import java.util.HashMap
import org.apache.log4j.helpers.LogLog
import com.signalcollect.implementations.serialization._

/**
 * MongoDB storage back end for persistent vertex storage {@link http://www.mongodb.org/}
 * Utilizes the casbah driver for MongoDB on scala. {@link http://api.mongodb.org/scala/casbah/current/}
 *
 * Uses MongoDB to Store the vertices and their outgoing edges on disk
 * 
 * >> Requires a running MongoDB deamon <<
 */
class MongoDBStorage(storage: Storage) extends VertexStore with DefaultSerializer {

  LogLog.setQuietMode(true) //make Log4J remain quiet

  /*
   * Connect to a MongoDB collection
   */
  val databaseName = "sc"
  val collectionID = RandomString("sc-", 16) //To make sure that different workers operate on different MongoDB collections
  var mongoStore = MongoConnection()(databaseName)(collectionID) //connect to localhost at port 27017 

  /**
   * Serializes a vertex object and adds it to the MongoDB collection
   *
   * @param vertex the vertex to store
   * @return insertion was successful i.e. the vertex was not already contained in the store
   */
  def put(vertex: Vertex): Boolean = {
    val builder = MongoDBObject.newBuilder
    builder += "id" -> vertex.id.toString
    if (mongoStore.findOne(builder.result) != None) {
      false
    } else {
      builder += "obj" -> write(vertex)
      mongoStore += builder.result
      true
    }

  }

  /**
   * Reads the serialized vertices from the database and reconstructs a vertex object
   *
   * @param id ID of the vertex to deserialize
   * @return the deserialized vertex
   */
  def get(id: Any): Vertex = {
    mongoStore.findOne(MongoDBObject("id" -> id.toString)) match {
      case Some(x) => val serialized = x.getAs[Array[Byte]]("obj"); read(serialized.get).asInstanceOf[Vertex]
      case _ => null
    }
  }

  /**
   * Deletes entries for vertices with this id
   *
   * @param id the id of the vertex to delete
   */
  def remove(id: Any) = {
    mongoStore.remove(MongoDBObject("id" -> id.toString))
    storage.toCollect.remove(id)
    storage.toSignal.remove(id)
  }

  /**
   * Replaces the serialized state of a vertex
   *
   * @param vertex the vertex that replaces the currently stored vertex with the same id
   */
  def updateStateOfVertex(vertex: Vertex) = {
    val q = MongoDBObject("id" -> vertex.id.toString)
    val serialized = write(vertex)
    val updated = MongoDBObject("id" -> vertex.id.toString, "obj" -> serialized)
    mongoStore.update(q, updated)
  }

  /**
   * Applies the specified function on all stored vertices and saves their changed state.
   * 
   * @param f the function to apply to all vertices in the database
   */
  def foreach[U](f: (Vertex) => U) {
    mongoStore.foreach(dbobj => {
      val vertex = read(dbobj.getAs[Array[Byte]]("obj").get).asInstanceOf[Vertex]
      f(vertex)
      updateStateOfVertex(vertex)
    })
  }

  def size = mongoStore.size

  /**
   * Drops the collection (but not the database)
   */
  def cleanUp {
    mongoStore.drop
  }
}

/**
 * Allows mixing in MongoDB in the DefaultStorage Implementation to change its storage back end.
 */
trait MongoDB extends DefaultStorage {
  override protected def vertexStoreFactory = new MongoDBStorage(this)
}