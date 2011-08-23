/*
 *  @author Philip Stutz
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

package com.signalcollect.javaapi;

import com.signalcollect.interfaces.*;

import scala.collection.JavaConversions;
import java.util.LinkedList;

@SuppressWarnings("serial")
public abstract class DataFlowVertexWithStateResetAfterSignaling<IdTypeParameter, StateTypeParameter, SignalTypeParameter>
		extends
		JavaVertexWithResetStateAfterSignaling<IdTypeParameter, StateTypeParameter, SignalTypeParameter> {

	public DataFlowVertexWithStateResetAfterSignaling(IdTypeParameter vertexId,
			StateTypeParameter initialState, StateTypeParameter resetState) {
		super(vertexId, initialState, resetState);
	}

	protected Iterable<SignalMessage<Object, IdTypeParameter, SignalTypeParameter>> uncollectedMessages = new LinkedList<SignalMessage<Object, IdTypeParameter, SignalTypeParameter>>();

	public void executeCollectOperation(
			scala.collection.Iterable<SignalMessage<?, ?, ?>> signalMessages,
			MessageBus<Object> messageBus) {
		LinkedList<SignalMessage<Object, IdTypeParameter, SignalTypeParameter>> newUncollectedMessages = new LinkedList<SignalMessage<Object, IdTypeParameter, SignalTypeParameter>>();
		Iterable<SignalMessage<?, ?, ?>> javaMessages = JavaConversions
				.asJavaIterable(signalMessages);
		LinkedList<SignalTypeParameter> uncollectedSignals = new LinkedList<SignalTypeParameter>();
		for (SignalMessage<?, ?, ?> message : javaMessages) {
			@SuppressWarnings("unchecked")
			SignalTypeParameter castSignal = (SignalTypeParameter) message
					.signal();
			uncollectedSignals.add(castSignal);
			@SuppressWarnings("unchecked")
			SignalMessage<Object, IdTypeParameter, SignalTypeParameter> castMessage = (SignalMessage<Object, IdTypeParameter, SignalTypeParameter>) message;
			newUncollectedMessages.add(castMessage);
		}
		uncollectedMessages = newUncollectedMessages;
		setState(collect(getState(), uncollectedSignals));
	}

	public abstract StateTypeParameter collect(StateTypeParameter oldState,
			Iterable<SignalTypeParameter> uncollectedSignals);

}