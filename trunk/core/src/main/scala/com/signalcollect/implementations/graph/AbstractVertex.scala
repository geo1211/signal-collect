/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.implementations.graph

import com.signalcollect.implementations.messaging.AbstractMessageRecipient
import com.signalcollect.api.factory._
import com.signalcollect.interfaces._
import com.signalcollect.implementations.messaging.AbstractMessageRecipient

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Buffer
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map
import scala.collection.GenMap

import java.util.LinkedList

abstract class AbstractVertex extends Vertex {

  /**
   * hashCode is cached for better performance
   */
  override val hashCode = this.id.hashCode

  protected def process(message: SignalMessage[_, _, _]) = {}

  def afterInitialization(messageBus: MessageBus[Any]) = {}

  /**
   * Access to the outgoing edges is required for some calculations and for executing the signal operations
   */
  protected var outgoingEdges: GenMap[EdgeId[Id, _], Edge] = HashMap[EdgeId[Id, _], Edge]()

  /** The state of this vertex when it last signaled. */
  protected var lastSignalState: Option[State] = None

  /** Keeps track if edges get added so this vertex remembers to signal for those */
  protected var outgoingEdgeAddedSinceSignalOperation = false

  /**
   * Adds a new outgoing {@link Edge} to this {@link FrameworkVertex}.
   * @param e the edge to be added.
   */
  def addOutgoingEdge(e: Edge): Boolean = {
    val edgeId = e.id.asInstanceOf[EdgeId[Id, Any]]
    if (!outgoingEdges.get(edgeId).isDefined) {
      outgoingEdgeAddedSinceSignalOperation = true
      outgoingEdges += ((edgeId, e))
      e.onAttach(this.asInstanceOf[e.SourceVertex])
      true
    } else {
      false
    }
  }

  /**
   * Removes an outgoing {@link Edge} from this {@link FrameworkVertex}.
   * @param e the edge to be added.
   */
  def removeOutgoingEdge(edgeId: EdgeId[_, _]): Boolean = {
    val castEdgeId = edgeId.asInstanceOf[EdgeId[Id, _]]
    val optionalOutgoinEdge = outgoingEdges.get(castEdgeId)
    if (optionalOutgoinEdge.isDefined) {
      val outgoingEdge = optionalOutgoinEdge.get
      outgoingEdges -= castEdgeId
      true
    } else {
      false
    }
  }

  /**
   * Removes all outgoing {@link Edge}s from this {@link Vertex}.
   * @return returns the number of {@link Edge}s that were removed.
   */
  def removeAllOutgoingEdges: Int = {
    val edgesRemoved = outgoingEdges.size
    outgoingEdges foreach ((tuple: (EdgeId[Id, Any], Edge)) => removeOutgoingEdge(tuple._1))
    edgesRemoved
  }

  /**
   * This method tells this {@link FrameworkVertex} to execute the signal operation
   * on all its outgoing {@Edge}s. This method is going to be
   * called by the SignalCollect framework during its execution (i.e. the
   * {@link Worker} implementations).
   *
   * @see Worker
   * @see Edge#executeSignalOperation
   */
  def executeSignalOperation(messageBus: MessageBus[Any]) {
    outgoingEdgeAddedSinceSignalOperation = false
    lastSignalState = Some(state)
    doSignal(messageBus)
  }

  def doSignal(messageBus: MessageBus[Any]) {
    outgoingEdges.foreach(_._2.executeSignalOperation(this, messageBus))
  }

  /**
   * The abstract "collect" function is algorithm specific and has to be implemented by a user of the API
   * this function will be called during algorithm execution. It is meant to calculate a new vertex state
   * based on the {@link Signal}s received by this vertex.
   */
  def collect(signals: Iterable[Signal]): State

  /**
   * Adds the buffered signals for that vertex and executes the {@link #collect} method on this vertex.
   * @see #collect
   * @param signals Buffered Signals for this vertex
   */
  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any])

  /**
   * This method is used by the framework in order to decide if the vertex' collect operation
   * should be executed.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect(signals: Iterable[SignalMessage[_, _, _]]): Double = signals.size

  /**
   * This method is used by the framework in order to decide if the vertex' signal operation should be executed.
   * The higher the returned value the more likely the vertex will be scheduled for executing its signal method.
   * @return the score value. The meaning of this value depends on the thresholds set in {@link ComputeGraph#execute}.
   */
  def scoreSignal: Double = {
    if (outgoingEdgeAddedSinceSignalOperation) {
      1
    } else {
      lastSignalState match {
        case None => 1
        case Some(oldState) => {
          if (oldState.equals(state))
            0
          else
            1
        }
      }
    }
  }

  /**
   * Returns the ids of all vertices to which this vertex has an outgoing edge
   */
  def getVertexIdsOfSuccessors: Iterable[_] = outgoingEdges.seq map (_._2.id.targetId)
  
  /**
   * Returns all outgoing edges
   */
  override def getOutgoingEdges: Option[Iterable[Edge]] = Some(outgoingEdges.seq map (_._2))
  
  /** Returns the number of outgoing edges of this [com.signalcollect.interfaces.Vertex] */
  def outgoingEdgeCount = outgoingEdges.size

  /**
   * Returns "VertexClassName(id=ID, state=STATE)"
   */
  override def toString: String = {
    this.getClass.getSimpleName + "(id=" + id + ", state=" + state + ")"
  }
}