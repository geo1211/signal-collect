package com.signalcollect.configuration.ssh

import java.io.File
import java.io.ByteArrayInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.StreamGobbler

class SshShell(
  username: String = "franciscodefreitas",
  hostname: String = "ifidyn185",
  port: Int = 22,
  privateKeyFilePath: String = System.getProperty("user.home") + System.getProperty("file.separator") + ".ssh" + System.getProperty("file.separator") + "id_rsa") {

  var connection = new Connection(hostname, port)
  connection.connect
  connection.authenticateWithPublicKey(username, new File(privateKeyFilePath), null)

  def execute(command: String): String = {
    val session = connection.openSession
    session.execCommand(command)
    val result = IoUtil.streamToString(new StreamGobbler(session.getStdout)) + "\n" + IoUtil.streamToString(new StreamGobbler(session.getStderr))
    session.close
    result
  }

  def exit {
    connection.close
  }

}