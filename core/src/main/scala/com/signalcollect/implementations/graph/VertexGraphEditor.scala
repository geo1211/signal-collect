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

import com.signalcollect.interfaces._
import com.signalcollect.GraphEditor

trait VertexGraphEditor extends AbstractVertex{
  
  protected var graphEditor: GraphEditor = _
  
  override def afterInitialization(messageBus: MessageBus[Any]) = {
    graphEditor = DefaultGraphEditor.createInstance(messageBus)
    super.afterInitialization(messageBus)
  }

}