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
import org.totalgrid.reef.broker._
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.reactor.ReactActor

import org.totalgrid.reef.loader.configuration._

import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.sapi.client.RestOperations

import org.totalgrid.reef.util.{ FileConfigReader, Logging, XMLHelper }
import java.io.File
import org.totalgrid.reef.proto.ReefServicesList

object LoadManager extends Logging {

  /**
   * TODO: Catch file not found exceptions and call usage.
   */
  def loadFile(client: => RestOperations, filename: String, benchmark: Boolean, dryRun: Boolean, ignoreWarnings: Boolean = false) = {

    info("Loading configuration file '" + filename + "'")

    val file = new File(filename)
    try {

      val xml = XMLHelper.read(file, classOf[Configuration])

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

  }

  def loadConfiguration(client: ModelLoader, xml: Configuration, benchmark: Boolean, path: File = new File(".")): Boolean = {

    var equipmentPointUnits = HashMap[String, String]()
    val actionModel = HashMap[String, ActionSet]()

    if (!xml.isSetEquipmentModel && !xml.isSetCommunicationsModel && !xml.isSetMessageModel)
      throw new Exception("No equipmentModel, communicationsModel, or messageModel. Nothing to do.")

    val loadCache = new LoadCache
    val ex = new LoadingExceptionCollector
    try {
      if (xml.isSetMessageModel) {
        val messageLoader = new MessageLoader(client, ex)
        val messageModel = xml.getMessageModel
        messageLoader.load(messageModel)
      }

      if (xml.isSetActionModel) {
        val actionSets = xml.getActionModel.getActionSet.toList
        actionSets.foreach(as => actionModel += (as.getName -> as))
      }

      if (xml.isSetEquipmentModel) {
        val equLoader = new EquipmentLoader(client, loadCache.loadCacheEqu, ex)
        val equModel = xml.getEquipmentModel
        equipmentPointUnits = equLoader.load(equModel, actionModel)
      }

      if (xml.isSetCommunicationsModel) {
        val comLoader = new CommunicationsLoader(client, loadCache.loadCacheCom, ex)
        val comModel = xml.getCommunicationsModel
        comLoader.load(comModel, path, equipmentPointUnits, benchmark)
      }
    } catch {
      case exception: Exception =>
        println("Parsing halted by terminal error: " + exception.getMessage)
        println("Fix Critical Errors and try again.")
        warn(exception.getStackTraceString)
    }

    val errors = ex.getErrors

    if (errors.size > 0) {
      println
      println("Critical Errors found:")
      errors.foreach(println(_))
      println
      false
    } else {
      loadCache.validate
    }
  }

  def run(amqp: AMQPProtoFactory, filename: String, benchmark: Boolean, dryRun: Boolean): Unit = {
    try {
      // we only connect to amqp if we are not doing a dry run
      def client = {
        amqp.connect(5000)

        // client that lets us talk to all the services through 1 interface
        val client = new ProtoClient(amqp, ReefServicesList, 5000)

        // get an auth token and attach it to the client for all future requests
        val authToken = client.put(ApplicationEnroller.buildLogin()).await().expectOne
        val env = new RequestEnv
        env.addAuthToken(authToken.getToken)
        client.setDefaultHeaders(env)

        client
      }

      loadFile(client, filename, benchmark, dryRun)

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
    var dryRun = false

    println("main: " + args.mkString(","))

    if (args.size < 2 || args.size > 4)
      usage

    try {

      while (!args.isEmpty) {

        args.head match {
          case "-c" | "-configuration" =>
            args = more(args)
            filename = Some(args.head)
          case "-benchmark" =>
            benchmark = true
          case "-dryRun" =>
            dryRun = true
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

    val dbInfo = Option(java.lang.System.getProperty("config")).map(f => BrokerProperties.get(new FileConfigReader(f))).getOrElse(BrokerConnectionInfo.loadInfo)
    val amqp = new AMQPProtoFactory with ReactActor {
      val broker = new QpidBrokerConnection(dbInfo)
    }

    run(amqp, filename.get, benchmark, dryRun)
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
    println("  -dryRun            Only validate the file, dont upload to server")
    println("")
    java.lang.System.exit(-1)
  }

}

