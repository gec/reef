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

import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.settings.{ AmqpSettings, UserSettings }
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.loader.commons.LoaderServicesImpl
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.factory.ReefConnectionFactory

object StandaloneLoader {
  def run(connectionInfo: AmqpSettings, userSettings: UserSettings, filename: String, benchmark: Boolean, dryRun: Boolean, ignoreWarnings: Boolean): Unit = {
    var cancelable = Option.empty[Cancelable]
    try {
      // we only connect to amqp if we are not doing a dry run
      def client = {
        val factory = new ReefConnectionFactory(connectionInfo, new ReefServices)
        val conn = factory.connect

        val client = conn.login(userSettings)

        cancelable = Some(new Cancelable {
          def cancel() = {
            factory.terminate()
          }
        })

        new LoaderServicesImpl(client)
      }

      LoadManager.loadFile(client, filename, benchmark, dryRun, ignoreWarnings)

    } finally {
      cancelable.foreach { _.cancel }
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
    var ignoreWarnings = false

    var propertyFiles = List.empty[String]

    try {

      while (!args.isEmpty) {

        args.head match {
          case "-c" | "-configuration" =>
            args = more(args)
            filename = Some(args.head)
          case "-benchmark" =>
            benchmark = true
          case "-ignoreWarnings" =>
            ignoreWarnings = true
          case "-dryRun" =>
            dryRun = true
          case "--configFile" =>
            args = more(args)
            propertyFiles = List(args.head) ::: propertyFiles
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

    if (propertyFiles.isEmpty) propertyFiles = List("target.cfg")

    import scala.collection.JavaConversions._
    val props = PropertyReader.readFromFiles(propertyFiles)

    val qpidConfig = new AmqpSettings(props)
    val userConfig = new UserSettings(props)

    run(qpidConfig, userConfig, filename.get, benchmark, dryRun, ignoreWarnings)
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
    println("  -ignoreWarnings    Ignore warnings")
    println("  -configFile <userfile> Path to *.cfg file(s)")
    println("")
    java.lang.System.exit(-1)
  }
}