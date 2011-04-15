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
import org.totalgrid.reef.reactor.ReactActor

import org.totalgrid.reef.loader.configuration._

import org.totalgrid.reef.api.{ ServiceHandlerHeaders, RequestEnv }
import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders
import org.totalgrid.reef.api.scalaclient.SyncOperations

import org.totalgrid.reef.util.{ FileConfigReader, Logging, XMLHelper }
import java.io.{ File, FileReader }
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.ReefServicesList

object LoadManager extends Logging {

  /**
   * TODO: Catch file not found exceptions and call usage.
   */
  def loadFile(client: SyncOperations, filename: String, benchmark: Boolean, dryRun: Boolean, ignoreWarnings: Boolean = false) = {

    info("Loading configuration file '" + filename + "'")

    val file = new File(filename)
    val reader = new FileReader(file)
    try {

      val xml = XMLHelper.read(reader, classOf[Configuration])

      val loader = new CachingModelLoader(None)

      val valid = loadConfiguration(loader, xml, benchmark, file.getParentFile)

      info("Finished analyzing configuration '" + filename + "'")

      if (!valid && !ignoreWarnings) {
        println("Configuration invalid, fix errors or add ignoreWarnings argument")
      } else if (!dryRun) {
        println("Uploading: " + loader.size + " objects to server...")
        loader.flush(client)
        println("Configuration loaded.")
      } else {
        println("DRYRUN: Skipping upload of " + loader.size + " objects.")
      }

    } catch {
      case ex =>
        println("Error loading configuration file '" + filename + "' " + ex.getMessage)
        throw ex
    }
    finally {
      reader.close
    }

  }

  def loadConfiguration(client: ModelLoader, xml: Configuration, benchmark: Boolean, path: File = new File(".")): Boolean = {

    var equipmentPointUnits = HashMap[String, String]()
    val actionModel = HashMap[String, ActionSet]()

    if (!xml.isSetEquipmentModel && !xml.isSetCommunicationsModel && !xml.isSetMessageModel)
      throw new Exception("No equipmentModel, communicationsModel, or messageModel. Nothing to do.")

    if (xml.isSetMessageModel) {
      val messageLoader = new MessageLoader(client)
      val messageModel = xml.getMessageModel
      messageLoader.load(messageModel)
    }

    if (xml.isSetActionModel) {
      val actionSets = xml.getActionModel.getActionSet.toList
      actionSets.foreach(as => actionModel += (as.getName -> as))
    }

    val loadCache = new LoadCache

    if (xml.isSetEquipmentModel) {
      val equLoader = new EquipmentLoader(client, loadCache.loadCacheEqu)
      val equModel = xml.getEquipmentModel
      equipmentPointUnits = equLoader.load(equModel, actionModel)
    }

    if (xml.isSetCommunicationsModel) {
      val comLoader = new CommunicationsLoader(client, loadCache.loadCacheCom)
      val comModel = xml.getCommunicationsModel
      comLoader.load(comModel, path, equipmentPointUnits, benchmark)
    }

    loadCache.validate
  }

  def run(amqp: AMQPProtoFactory, filename: String, benchmark: Boolean): Unit = {

    amqp.connect(5000)

    try {
      // client that lets us talk to all the services through 1 interface
      val client = new ProtoClient(amqp, ReefServicesList, 5000)

      // get an auth token and attach it to the client for all future requests
      val authToken = client.putOneOrThrow(ApplicationEnroller.buildLogin())
      val env = new RequestEnv
      env.addAuthToken(authToken.getToken)
      client.setDefaultHeaders(env)

      loadFile(client, filename, benchmark, false)
    } finally {
      amqp.disconnect(5000)
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

