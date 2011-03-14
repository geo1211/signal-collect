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

package ch.uzh.ifi.ddis.signalcollect.graphproviders

class SparqlTuples(
  db: SparqlAccessor,
  query: String,
  sourceBindingName: String = "source",
  targetBindingName: String = "target"
) extends Traversable[(String, String)] {
  def foreach[U](f: ((String, String)) => U) = {
      val bindings = db.execute(query)
      for (binding <- bindings) {
        val sourceRdfTerm: String = binding.get(sourceBindingName) match {
          case Some(binding) => binding
          case None => throw new Exception("Source RDF term not bound: " + sourceBindingName)
        }
        val targetRdfTerm: String = binding.get(targetBindingName) match {
          case Some(binding) => binding
          case None => throw new Exception("Target RDF term not bound: " + targetBindingName)
        }
        f(sourceRdfTerm, targetRdfTerm)
      }
  }
}