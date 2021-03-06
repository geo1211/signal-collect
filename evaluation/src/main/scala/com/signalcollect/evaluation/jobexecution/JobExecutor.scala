/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
 *  @author Francisco de Freitas
 *  @author Lorenz Fischer
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

package com.signalcollect.evaluation.jobexecution

import org.apache.commons.codec.binary.Base64
import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import com.signalcollect.evaluation.spreadsheets._
import java.util.Date
import java.text.SimpleDateFormat
import com.signalcollect.evaluation.configuration._
import scala.util.Random
import com.signalcollect.graphproviders.synthetic.LogNormal
import com.signalcollect.implementations.serialization.DefaultSerializer
import java.io.ObjectInputStream
import java.io.FileInputStream
import java.io.File

object JobExecutor extends App {
  var job: Job = _
  if (args.size > 0) {
    try {
      val jobId = args(0).toInt
      val configFile = new File(jobId + ".config")
      val jobArray = new Array[Byte](configFile.length.toInt)
      val fileInputStream = new FileInputStream(configFile)
      fileInputStream.read(jobArray)
      job = DefaultSerializer.read[Job](jobArray)
    } catch {
      case e: Exception => throw new Exception("Could not load configuration: " + e.getStackTrace)
    }
  } else {
    throw new Exception("No jobId specified.")
  }
  val executor = new JobExecutor
  executor.run(job)
}

class JobExecutor {
  def run(job: Job) {
    var statsMap = Map[String, String]()
    try {
      statsMap = job.execute()
      statsMap += (("evaluationDescription", job.jobDescription))
      statsMap += (("submittedByUser", job.submittedByUser))
      statsMap += (("jobId", job.jobId.toString))
      statsMap += (("executionHostname", java.net.InetAddress.getLocalHost.getHostName))
      statsMap += (("java.runtime.version", System.getProperties.get("java.runtime.version").toString))
      if (job.spreadsheetConfiguration.isDefined) {
        submitSpreadsheetRow(job.spreadsheetConfiguration.get, statsMap)
      } else {
        println(statsMap)
      }
    } catch {
      case e: Exception =>
        println(statsMap)
        sys.error(e.getMessage + "\n" + e.getStackTraceString)
    }

    def submitSpreadsheetRow(spreadsheetConfig: SpreadsheetConfiguration, rowData: Map[String, String]) {
      val api = new SpreadsheetApi(spreadsheetConfig.gmailAccount, spreadsheetConfig.gmailPassword)
      val spreadsheet = api.getSpreadsheet(spreadsheetConfig.spreadsheetName)
      val worksheet = api.getWorksheetInSpreadsheet(spreadsheetConfig.worksheetName, spreadsheet)
      api.insertRow(worksheet, statsMap)
    }
  }
}
