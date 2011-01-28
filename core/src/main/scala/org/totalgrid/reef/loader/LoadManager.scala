/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.loader

import org.totalgrid.reef.app.ApplicationEnroller

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging._
import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection
import org.totalgrid.reef.reactor.{ ReactActor, LifecycleManager, Lifecycle }

import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.loader.equipment.EquipmentModel
import org.totalgrid.reef.loader.communications.CommunicationsModel

import org.totalgrid.reef.messaging.ServiceHandlerHeaders._
import org.totalgrid.reef.util.{ FileConfigReader, Logging, XMLHelper }
import java.io.{ File, FileReader }
import org.totalgrid.reef.util.Logging

object LoadManager extends Logging {

  /**
   * TODO: Catch file not found exceptions and call usage.
   */
  def load(client: SyncServiceClient, filename: String, benchmark: Boolean) = {

    info("Loading configuration '" + filename + "'")
    try {
      val file = new File(filename)
      val xml = XMLHelper.read(new FileReader(file), classOf[Configuration])
      var equipmentPointUnits = HashMap[String, String]()
      val actionModel = HashMap[String, ActionSet]()

      if (!xml.isSetEquipmentModel && !xml.isSetCommunicationsModel)
        throw new Exception("No EquipmentModel or CommunicationsModel. Nothing to do.")

      if (xml.isSetActionModel) {
        val actionSets = xml.getActionModel.getActionSet.toList
        actionSets.foreach(as => actionModel += (as.getName -> as))
      }

      if (xml.isSetEquipmentModel) {
        val equLoader = new EquipmentLoader(client)
        val equModel = xml.getEquipmentModel
        equipmentPointUnits = equLoader.load(equModel, actionModel)
      }

      if (xml.isSetCommunicationsModel) {
        val comLoader = new CommunicationsLoader(client)
        val comModel = xml.getCommunicationsModel
        comLoader.load(comModel, file.getParentFile, equipmentPointUnits, benchmark)
      }

      info("Finished loading configuration '" + filename + "'")

    } catch {
      case ex =>
        println("Error loading configuration file '" + filename + "' " + ex.getMessage)
        throw ex
    }

  }

  def run(amqp: AMQPProtoFactory, filename: String, benchmark: Boolean): Unit = {

    amqp.start

    try {
      // client that lets us talk to all the services through 1 interface
      val client = new ProtoClient(amqp, 5000, ServicesList.getServiceInfo)

      // get an auth token and attach it to the client for all future requests
      val authToken = client.put_one(ApplicationEnroller.buildLogin())
      val env = new RequestEnv
      env.addAuthToken(authToken.getToken)
      client.setDefaultEnv(env)

      load(client, filename, benchmark)
    } finally {
      amqp.stop
    }
  }

  /**
   * loader [-benchmark] -c[onfiguration] <configuration.xml>
   * @see usage
   *
   * TODO: Might use this code for args parsing: https://github.com/paulp/optional
   */
  def main(argsA: Array[String]): Unit = {

    var args = argsA.toList
    var filename: Option[String] = None
    var benchmark = false

    println("main: " + args.mkString(","))

    if (args.size < 2 || args.size > 3)
      usage

    try {

      while (!args.isEmpty) {

        args.head match {
          case "-c" | "-configuration" =>
            args = more(args)
            filename = Some(args.head)
          case "-benchmark" =>
            benchmark = true
        }
        args = args drop 1
      }

    } catch {
      case ex =>
        printf("Exception: " + ex.toString)
        usage
    }

    if (filename == None)
      usage

    val dbInfo = Option(java.lang.System.getProperty("config")).map(f => AMQPProperties.get(new FileConfigReader(f))).getOrElse(BrokerConnectionInfo.loadInfo)
    val amqp = new AMQPProtoFactory with ReactActor {
      val broker = new QpidBrokerConnection(dbInfo)
    }

    run(amqp, filename.get, benchmark)
  }

  /**
   * Pop the used arg off the front of the list and get one more.
   * If there isn't one more, the user didn't supply the correct
   * number.
   */
  def more(args: List[String]): List[String] = {
    val args2 = args drop 1
    if (args2.isEmpty)
      usage
    args2
  }

  def usage: Unit = {

    println("usage: loader [-benchmark] -c[onfiguration] <configuration.xml>")
    println("Load a configuration.")
    println("OPTIONS:")
    //println("12345678901234567890123456789012345678901234567890123456789012345678901234567890")
    //println("  -dir <config_dir>  Load all configuration files found in config_dir")
    println("  -c             <configuration.xml>")
    println("  -configuration <configuration.xml>")
    println("                     Load configuration data from <configuration.xml>")
    println("  -benchmark         Override endpoint protocol to force all endpoints in")
    println("                     configuration file to be simulated.")
    println("")
    java.lang.System.exit(-1)
  }

}

