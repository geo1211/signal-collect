package com.signalcollect


import com.signalcollect.api._
import com.signalcollect.api.factory._
import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.graphproviders._
import com.signalcollect.graphproviders.Grid
import com.signalcollect.examples._
import com.signalcollect.implementations.logging.DefaultLogger


class VerifiedColoredVertex(id: Int, numColors: Int) extends ColoredVertex(id, numColors, 0, false) {
  // only necessary to allow access to vertex internals
  def publicMostRecentSignals: Iterable[Int] = mostRecentSignalMap.values.asInstanceOf[Iterable[Int]]
}