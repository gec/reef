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
package org.totalgrid.reef.standalone

import org.totalgrid.reef.models.CoreServicesSchema
import org.totalgrid.reef.services.ServiceBootstrap
import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector, DbConnection }
import org.apache.commons.cli._
import org.apache.commons.cli
import org.totalgrid.reef.client.settings.util.PropertyReader
import java.io.{ InputStreamReader, BufferedReader }
import scala.annotation.tailrec
import org.totalgrid.reef.client.settings.UserSettings
import net.agileautomata.executor4s._
import com.typesafe.scalalogging.slf4j.Logging

object ResetDbEntryPoint extends Logging {

  def main(args: Array[String]) {
    try {
      handle(args)
    } catch {
      case ex: Throwable =>
        println(ex.getMessage)
        logger.error(ex.toString)
        System.exit(1)
    }
  }

  def handle(args: Array[String]) {
    val options = buildOptions
    val parser: CommandLineParser = new BasicParser

    val line = parser.parse(options, args)

    if (line.hasOption("h")) {
      (new HelpFormatter).printHelp("resetdb", options)
    } else {
      val properties = PropertyReader.readFromFile("standalone-node.cfg")

      if (line.hasOption("p") && line.hasOption("i")) {
        println("Choose one of either -p or -i")
        System.exit(1)
      }

      val systemPassword = if (line.hasOption("p")) {
        println("WARNING: password will be visible in command history")
        line.getOptionValue("p").trim
      } else if (line.hasOption("i")) {
        promptForPassword()
      } else {
        val userSettings = new UserSettings(properties)
        if (userSettings.getUserName == "system") {
          println("Using system password from configuration file.\nUse -p or -i to manually provide password.")
          userSettings.getUserPassword
        } else {
          throw new IllegalArgumentException("Could not determine password, use either -p or -i to provide it.")
        }
      }

      val clearFirst = line.hasOption("hard")
      val useSquerylReset = line.hasOption("old")

      val sql = new DbInfo(properties)
      val dbConnection = DbConnector.connect(sql)

      val exe = Executors.newResizingThreadPool(5.minutes)
      val mstore = ImplLookup.loadMeasurementStore(properties, exe)

      resetdb(dbConnection, mstore, systemPassword, clearFirst, useSquerylReset)
    }
  }

  def buildOptions: Options = {
    val opts = new Options
    opts.addOption("h", "help", false, "Display this help text")
    opts.addOption("p", "password", true, "Password for non-interactive scripting. WARNING: password will be visible in command history")
    opts.addOption("i", "ask-password", false, "Prompt new system password")
    opts.addOption(new cli.Option(null, "hard", false, "Clear the database first"))
    opts.addOption(new cli.Option(null, "old", false, "Migrate database schema (rather than clearing then writing)"))
    opts
  }

  @tailrec
  def promptForPassword(): String = {
    val stdIn = new BufferedReader(new InputStreamReader(System.in))
    System.out.println("Enter new system password: ")
    val p1 = stdIn.readLine.trim
    System.out.println("Verify system password: ")
    val p2 = stdIn.readLine.trim
    if (p1 != p2) {
      println("Passwords do not match, please try again.")
      promptForPassword()
    } else {
      p2
    }
  }

  def resetdb(dbConnection: DbConnection, mstore: MeasurementStore, systemPassword: String, clearFirst: Boolean, useSquerylReset: Boolean) {
    mstore.connect()

    try {
      try {
        CoreServicesSchema.prepareDatabase(dbConnection, clearFirst, !useSquerylReset)
      } catch {
        case ex: CoreServicesSchema.FirstMigrationNeededException =>
          println("Switching to migration based database, assuming --hard onetime only")
          CoreServicesSchema.prepareDatabase(dbConnection, true, !useSquerylReset)
      }

      clearFirst match {
        case true => println("Cleared and updated jvm database")
        case false => println("Updated database")
      }

      ServiceBootstrap.seed(dbConnection, systemPassword)
      println("Updated default agents and events")

      if (clearFirst) {
        if (mstore.reset) {
          println("Cleared measurement store")
        } else {
          println("NOTE: measurement store not reset, needs to be done manually")
        }
      }
    } catch {
      case ex: Throwable => println("Reset failed: " + ex.toString)
    }

    mstore.disconnect()
  }
}
