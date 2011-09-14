package com.signalcollect.aws

import scala.collection.JavaConversions._
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.auth.PropertiesCredentials
import java.io.File
import com.amazonaws.services.ec2.model.DescribeReservedInstancesRequest
import com.amazonaws.services.ec2.model.GetPasswordDataRequest
import com.amazonaws.services.ec2.model.DescribeAddressesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.StartInstancesRequest
import com.amazonaws.services.ec2.model.ActivateLicenseRequest
import com.signalcollect.evaluation.jobsubmission.SshShell
import com.amazonaws.services.ec2.model.PurchaseReservedInstancesOfferingRequest
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.MonitorInstancesRequest
import com.amazonaws.services.ec2.model.StopInstancesRequest

object Ec2Deploy extends App {
  val running = 16

  val ec2: AmazonEC2 = new AmazonEC2Client(new PropertiesCredentials(new File("./AwsCredentials.properties")))
  val regions = ec2.describeRegions
  println(regions)
  ec2.setEndpoint("ec2.eu-west-1.amazonaws.com")
  //  ec2.setEndpoint("ec2.us-east-1.amazonaws.com")
  //  val startRequest = (new StartInstancesRequest).withInstanceIds("i-4cdcc23a")
  //  ec2.startInstances(startRequest)
  //  ec2.runInstances(new RunInstancesRequest)
  //  val monitorResult = ec2.monitorInstances((new MonitorInstancesRequest).withInstanceIds("i-4cdcc23a"))
  val descriptions = ec2.describeInstances
  if (numberOfRunningInstances(ec2) == 0) {
    println("No reservations, starting a machine ...")
    ec2.runInstances((new RunInstancesRequest).withInstanceType("t1.micro").withImageId("ami-109ba864").withKeyName("aws").withMaxCount(1).withMinCount(1).withSecurityGroupIds("sg-e42f3e90"))
  } else {
    for (reservation <- descriptions.getReservations) {
      val instances = reservation.getInstances
      for (instance <- instances) {
        println(instance)
        val state = instance.getState
        if (state.getCode == running) {
          println("Instance " + instance.getInstanceId + " is running @ address " + instance.getPublicIpAddress)
          println("Attempting to ssh into instance ...")
          val shell = new SshShell(username = "ec2-user", hostname = instance.getPublicIpAddress, port = 22, privateKeyFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + ".ec2" + System.getProperty("file.separator") + "aws.pem")
          println("Connected.")
          println(shell.execute("/usr/java/latest/bin/java -version"))
        }
      }
    }
  }

  def numberOfRunningInstances(ec2: AmazonEC2): Int = {
    var numberOfRunningInstances = 0
    val descriptions = ec2.describeInstances
    for (reservation <- descriptions.getReservations) {
      val instances = reservation.getInstances
      for (instance <- instances) {
        println(instance)
        val state = instance.getState
        if (state.getCode == running) {
          numberOfRunningInstances += 1
        }
      }
    }
    numberOfRunningInstances
  }

}
