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

package signalcollect.algorithms

import scala.collection.mutable.HashSet
import signalcollect.api._
import signalcollect.api.vertices._
import signalcollect.api.edges._

/**
 * Represents all associated Sudoku cells that have to be taken into account to determine
 * the value of a cell
 *
 */
class SudokuAssociation(s: Any, t: Any) extends DefaultEdge(s, t) {
  type SourceVertexType = SudokuCell
  def signal = source.state
}

/**
 * Cell in a Sudoku grid.
 *
 * @param id ID of the cell, where top left cell has id=0 top right has id of 8 and bottom right has id 80
 *
 */
class SudokuCell(id: Int, initialState: Option[Int] = None) extends SignalMapVertex(id, initialState) {
  
	var possibleValues = Set[Option[Int]]()
	initialState match {
		case Some(x) => possibleValues+=initialState
		case None => possibleValues = SudokuHelper.legalNumbers
	}
	
  def collect: Option[Int] = {
    //make a list of all possible values 
    possibleValues = possibleValues -- mostRecentSignals[Option[Int]].toSet
    
    //If own value is determined i.e. if only one possible value is left choose own value
    if (possibleValues.size == 1) {
      possibleValues.head
    }
    else
      state
  }
}

object sudoku {

  def main(args: Array[String]): Unit = {
    //Values that are given, rest has default value 'None'
    val initialSeed = Map(
      4 -> 9,
      5 -> 6,
      8 -> 5,
      10 -> 9,
      11 -> 4,
      13 -> 2,
      14 -> 1,
      15 -> 8,
      16 -> 6,
      19 -> 1,
      21 -> 4,
      24 -> 3,
      25 -> 2,
      29 -> 3,
      31 -> 4,
      34 -> 7,
      36 -> 1,
      38 -> 6,
      42 -> 4,
      44 -> 2,
      46 -> 4,
      49 -> 6,
      51 -> 5,
      55 -> 5,
      56 -> 2,
      59 -> 4,
      61 -> 1,
      64 -> 6,
      65 -> 1,
      66 -> 2,
      67 -> 3,
      69 -> 7,
      70 -> 8,
      72 -> 4,
      75 -> 8,
      76 -> 1)

    val cg = new AsynchronousComputeGraph()

    //Add all Cells for Sudoku
    for (index <- 0 to 80) {
      val seedValue = initialSeed.get(index)
      cg.addVertex[SudokuCell](index, seedValue)
    }

    //Determine neighboring cells for each cell and draw the edges between them 
    for (index <- 0 to 80) {
      SudokuHelper.cellsToConsider(index).foreach({ i =>
        cg.addEdge[SudokuAssociation](i, index)
      })
    }

    var seed = Map[Int, Option[Int]]()
    cg.foreach { v => seed += Pair(v.id.asInstanceOf[Int], v.state.asInstanceOf[Option[Int]]) }
    SudokuHelper.printSudoku(seed)

    val stats = cg.execute()
    println(stats)
    println()

    var result = Map[Int, Option[Int]]()
    cg.foreach { v => println(v); result += Pair(v.id.asInstanceOf[Int], v.state.asInstanceOf[Option[Int]]) }
    SudokuHelper.printSudoku(result)

    cg.shutDown
  }
}

/**
 * Provides useful utilites for dealing with sudoku grids
 *
 */
object SudokuHelper {
  //All possible numbers for a cell
  val legalNumbers = {
	  var numbers = Set[Option[Int]]()
	  for(i<-1 to 9) {
	 	  numbers += Some(i)
	  }
	   println
	   numbers
  }
  
  //Get Rows, Columns and Bocks from ID
  def getRow(id: Int) = id / 9
  def getColumn(id: Int) = id % 9
  def getBlock(id: Int) = getRow(id) * 3 + getColumn(id)

  /**
   * Returns all the neighboring cells that influence a cell's value
   */
  def cellsToConsider(id: Int): List[Int] = {
    var neighborhood = List[Int]()

    //Same row
    for (col <- 0 to 8) {
      val otherID = getRow(id) * 9 + col
      if (otherID != id) {
        neighborhood = otherID :: neighborhood
      }
    }

    //Same column
    for (row <- 0 to 8) {
      val otherID = row * 9 + getColumn(id)
      if (otherID != id) {
        neighborhood = otherID :: neighborhood
      }
    }

    //Same block
    val topLeftRow = (getRow(id) / 3) * 3
    val topLeftColumn = (getColumn(id) / 3) * 3

    for (row <- topLeftRow to (topLeftRow + 2)) {
      for (column <- topLeftColumn to (topLeftColumn + 2)) {
        val otherID = row * 9 + column

        if (otherID != id && !neighborhood.contains(otherID)) {
          neighborhood = otherID :: neighborhood
        }
      }
    }

    neighborhood
  }

  /**
   * Formats the data in a classical sudoku layout
   */
  def printSudoku(data: Map[Int, Option[Int]]) = {
	  
    println()
    println("Sudoku")
    println("======")
    println()
    println("=========================================")

    for (i <- 0 to 8) {
      val j = i * 9
      print("||")
      for (k <- j to j + 8) {
        data.get(k) match {
          case Some(Some(v)) => print(" " + v + " ")
          case v => print("   ") //Empty or Error
        }
        if (k % 3 == 2) {
          print("II")
        } else {
          print("|")
        }
      }
      println()
      if (i % 3 == 2) {
        println("=========================================")
      }
    }
  }
}