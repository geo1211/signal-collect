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

package ch.uzh.ifi.ddis.signalcollect.interfaces

trait Edge[SourceIdType, TargetIdType] {

  /** The identifier of the {@link Vertex} where this {@link Edge} originates from. */
  def sourceId: SourceIdType

  /** The identifier of the {@link Vertex} where this {@link Edge} points to. */
  def targetId: TargetIdType

  /**
   * The abstract "signal" function is algorithm specific and has to be implemented by a user of the API
   * this function will be called during algorithm execution. It is meant to calculate a signal
   * going from the source vertex of this edge to the target vertex of this edge.
   */
  def signal: SignalType

  /** The weight of this {@link Edge}. By default an {@link Edge} has a weight of <code>1</code>. */
  def weight: Double

  @specialized
  type SignalType = Any

  /** The identifier of this {@link Edge}. */
  def id: (SourceIdType, TargetIdType, String) = (sourceId, targetId, getClass.getName)

  /** The hash code of this object. */
  override def hashCode = id.hashCode

  /** The hash code of the target vertex. */
  def targetHashCode = targetId.hashCode

  /** The type of the source {@link Vertex} which can be found using {@link #sourceId}. */
  type SourceVertexType <: Vertex[_, _]

  /** Setter with an unsafe cast. The user is responsible for not setting vertex types that don't match in practice */
  def setSource(v: Vertex[_, _])

  /**
   * This method will be called by {@link FrameworkVertex#executeSignalOperation}
   * of this {@Edge} source vertex. It calculates the signal and sends it over the message bus.
   * {@link OnlySignalOnChangeEdge}.
   * 
   * @param mb the message bus to use for sending the signal
   */
  def executeSignalOperation(mb: MessageBus[Any, Any])

}