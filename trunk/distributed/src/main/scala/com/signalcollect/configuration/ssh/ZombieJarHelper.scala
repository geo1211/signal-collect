package com.signalcollect.configuration.ssh

import scala.util.Random
import scala.sys.process._
import java.io.File

class ZombieJarHelper(
  numWorkers: Int,
  user: String) {

  val jarDescription: String = Random.nextInt.abs.toString
  val pathToSignalcollectCorePom: String = new File("../core/pom.xml").getCanonicalPath // maven -file CLI parameter can't relative paths
  val mainClass: String = "com.signalcollect.Zombie"
  val packagename: String = "evaluation-0.0.1-SNAPSHOT"

  lazy val jarSuffix = "-jar-with-dependencies.jar"
  lazy val fileSpearator = System.getProperty("file.separator")
  lazy val localhostJarname = packagename + jarSuffix
  lazy val jarName = packagename + "-" + jarDescription + jarSuffix
  lazy val localJarpath = "." + fileSpearator + "target" + fileSpearator + localhostJarname
  
  prepareJar

  def prepareJar {

    val commandInstallCore = "mvn -file " + pathToSignalcollectCorePom + " -Dmaven.test.skip=true clean install"
    println(commandInstallCore)
    println(commandInstallCore !!)

    /** PACKAGE CODE AS JAR */
    val commandPackage = "mvn -Dmaven.test.skip=true clean package"
    println(commandPackage)
    println(commandPackage !!)

  }

  def copyJarToHosts(hosts: List[String]) {

    for (host <- hosts) {

      /** COPY JAR TO HOST */
      val commandCopy = "scp -v " + localJarpath + " " + user + "@" + host + ":" + jarName
      println(commandCopy)
      println(commandCopy !!)

    }

  }

  def startJarAtHost(host: String, coordinatorIp: String) {

    /** LOG INTO HOST WITH SSH */
    val hostShell = new SshShell(username = user, hostname = host)

    // "java -Xmx35000m -Xms35000m -jar " + jarName + " " + coordinatorIp

    val startCommand = """echo hahaha"""
    println(hostShell.execute(startCommand))

  }

}