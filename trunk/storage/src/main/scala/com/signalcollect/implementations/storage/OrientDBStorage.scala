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
 *  
 */

package com.signalcollect.implementations.storage

import com.signalcollect.implementations.storage.wrappers._
import com.signalcollect.interfaces._
import javax.persistence.{ Id, Version }
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLUpdate
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelete
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import com.orientechnologies.orient.core.db.`object`.{ ODatabaseObject, ODatabaseObjectPool, ODatabaseObjectTx }
import com.signalcollect.implementations.serialization._
import java.io.File

/**
 * OrientDB storage back end for persistent vertex storage {@link http://www.orientechnologies.com/orient-db.htm}
 * Needs no additional installation because it runs directly from jar
 */
class OrientDBStorage(storage: Storage, DBLocation: String) extends VertexStore with VertexSerializer {

  /**
   * Wrapper to allow SQL like queries
   */
  implicit def dbWrapper(db: ODatabaseObjectTx) = new {
    def queryBySql[T](sql: String, params: AnyRef*): List[T] = {
      val params4java = params.toArray
      val results: java.util.List[T] = db.query(new OSQLSynchQuery[T](sql), params4java: _*)
      results.asScala.toList
    }
  }

  /**
   *  Create DB or connect to existing
   */
  val dblocation = DBLocation + RandomString("", 5) + "/"
  val f = new File(dblocation).mkdir
  val uri: String = "local:" + dblocation
  var db: ODatabaseObjectTx = new ODatabaseObjectTx(uri)
  if (!db.exists) {
    db.create()
  } else {
    db.open("admin", "admin") //Default parameters
  }
  db.getEntityManager.registerEntityClasses("com.signalcollect.implementations.storage.wrappers") // Registers the adapter class

  /**
   * Reads the serialized vertices from the database and reconstructs a vertex object
   *
   * @param id ID of the vertex to deserialize
   * @return the deserialized vertex
   */
  def get(id: Any): Vertex = {
    val serialized = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", id.toString.asInstanceOf[AnyRef])
    if (serialized.isEmpty) {
      null
    } else {
      read(serialized.last.serializedVertex)
    }
  }

  /**
   * Serializes a vertex object and adds it to the database
   *
   * @param vertex the vertex to store
   * @return insertion was successful i.e. the vertex was not already contained in the database
   */
  def put(vertex: Vertex): Boolean = {
    var alreadyStored = false
    if (size > 0) {
      val queryResults = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", vertex.id.toString.asInstanceOf[AnyRef])
      alreadyStored = queryResults.size > 0
    }
    if (!alreadyStored) {
      db.save(new OrientWrapper(vertex.id.toString, write(vertex)))
      storage.toCollect.addVertex(vertex.id)
      storage.toSignal.add(vertex.id)
    } 
    !alreadyStored
  }

  /**
   * Deletes entries for vertices with this id
   *
   * @param id the id of the vertex to delete
   */
  def remove(id: Any) = {
    val queryResult = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", id.toString.asInstanceOf[AnyRef])
    if (!queryResult.isEmpty) {
      db.delete(queryResult.last)
      storage.toSignal.remove(id)
      storage.toCollect.remove(id)
    }
  }

  /**
   * Replaces the serialized state of a vertex
   *
   * @param vertex the vertex that replaces the currently stored vertex with the same id
   */
  def updateStateOfVertex(vertex: Vertex) = {
    var wrappedVertex = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", vertex.id.toString.asInstanceOf[AnyRef]).last
    wrappedVertex.serializedVertex = write(vertex)
    db.save(wrappedVertex)
  }

  /**
   * Applies the specified function on all stored vertices and saves their changed state.
   * 
   * @param f the function to apply to all vertices in the database
   */
  def foreach[U](f: (Vertex) => U) {
    val iterator = db.browseClass[OrientWrapper]("OrientWrapper")
    while (iterator.hasNext) {
      val vertex: Vertex = read(iterator.next.serializedVertex)
      f(vertex)
      updateStateOfVertex(vertex)
    }
  }

  def size: Long = db.countClass(classOf[OrientWrapper])

  /**
   * Deletes the whole database
   */
  def cleanUp {
    val folder = new File(dblocation)
    folder.listFiles.foreach(file => file.delete)
    folder.delete
  }
}

/**
 * To allow mixing-in this storage implementation into a more general storage implementation
 */
trait Orient extends DefaultStorage {
  override protected def vertexStoreFactory = {
    val currentDir = new File(".")
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId + "/")
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        new OrientDBStorage(this, torqueTempFolder.getAbsolutePath)
      } else {
        new OrientDBStorage(this, currentDir.getCanonicalPath + "/sc-orient/")
      }
    } else {
      new OrientDBStorage(this, currentDir.getCanonicalPath + "/sc-orient/")
    }
  }
}