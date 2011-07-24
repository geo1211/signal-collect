package com.signalcollect.configuration.ssh

import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

object IoUtil {

  def streamToString(in: InputStream): String = {
    val reader = new BufferedReader(new InputStreamReader(in))
    val builder = new StringBuilder
    var line = reader.readLine
    while (line != null) {
      builder.append(line)
      builder.append("\n")
      line = reader.readLine
    }
    builder.toString
  }

  def printStream(in: InputStream) {
    val reader = new BufferedReader(new InputStreamReader(in))
    val builder = new StringBuilder
    var line = reader.readLine
    while (line != null) {
      println(line)
      line = reader.readLine
    }
  }
  
}