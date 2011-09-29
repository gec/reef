/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.loader

import org.totalgrid.reef.messaging._
import org.totalgrid.reef.broker.api._
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.executor.ReactActorExecutor

import org.totalgrid.reef.sapi.RequestEnv

import org.totalgrid.reef.util.FileConfigReader

import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.proto.Auth.{ AuthToken, Agent }

object StandaloneLoader {
  def run(amqp: AMQPProtoFactory, filename: String, benchmark: Boolean, dryRun: Boolean, create: Boolean, username: String, password: String): Unit = {
    try {
      // we only connect to amqp if we are not doing a dry run
      def client = {
        amqp.connect(5000)

        // client that lets us talk to all the services through 1 interface
        val client = new AmqpClientSession(amqp, ReefServicesList, 5000)

        // get an auth token and attach it to the client for all future requests

        val agent = Agent.newBuilder.setName(username).setPassword(password).build
        val request = AuthToken.newBuilder.setAgent(agent).build

        val authToken = client.put(request).await().expectOne
        val env = new RequestEnv
        env.addAuthToken(authToken.getToken)
        client.setDefaultHeaders(env)

        client
      }

      LoadManager.loadFile(client, filename, benchmark, dryRun, false, create)

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
    var create = true
    var username = "system"
    var password = "system"

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
          case "-d" =>
            create = false
          case "-dryRun" =>
            dryRun = true
          case "-u" =>
            args = more(args)
            username = args.head
          case "-p" =>
            args = more(args)
            password = args.head
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

    val dbInfo = Option(java.lang.System.getProperty("config")).map(f =>
      BrokerProperties.get(new FileConfigReader(f))).getOrElse(BrokerConnectionInfo.loadInfo)
    val amqp = new AMQPProtoFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(dbInfo)
    }

    run(amqp, filename.get, benchmark, dryRun, create, username, password)
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
    println("  -d                 Delete represented model from server")
    println("  -u <username>      Set the username to load as.")
    println("  -p <password>      Set the password for username")
    println("")
    java.lang.System.exit(-1)
  }
}