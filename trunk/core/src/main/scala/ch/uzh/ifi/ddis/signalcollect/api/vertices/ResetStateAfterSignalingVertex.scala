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

package ch.uzh.ifi.ddis.signalcollect.api.vertices

import ch.uzh.ifi.ddis.signalcollect.implementations.graph.ResetStateAfterSignaling
import ch.uzh.ifi.ddis.signalcollect.interfaces._
import ch.uzh.ifi.ddis.signalcollect.implementations.graph.AbstractVertex
import ch.uzh.ifi.ddis.signalcollect.implementations.graph.UncollectedSignalsList
import ch.uzh.ifi.ddis.signalcollect.implementations.graph.MostRecentSignalMap
import scala.collection.mutable.Map
import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LinkedHashMap

abstract class ResetStateAfterSignalingVertex[IdType, StateType](id: IdType, val initialState: StateType) extends DefaultVertex[IdType, StateType](id, initialState) with ResetStateAfterSignaling[IdType, StateType]