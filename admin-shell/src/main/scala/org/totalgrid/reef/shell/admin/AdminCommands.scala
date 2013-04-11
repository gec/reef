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
package org.totalgrid.reef.shell.admin

import org.totalgrid.reef.shell.proto.ReefCommandSupport

import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.measurementstore.MeasurementStoreFinder
import org.totalgrid.reef.services.ServiceBootstrap
import org.apache.felix.gogo.commands.{ Option => GogoOption, Command }
import java.io.{ InputStreamReader, BufferedReader }
import org.totalgrid.reef.models.CoreServicesSchema
import org.totalgrid.reef.client.settings.UserSettings

@Command(scope = "reef", name = "resetdb", description = "Migrates the sql tables without deleting user data.")
class ResetDatabaseCommand extends ReefCommandSupport {

  @GogoOption(name = "-p", description = "password for non-interactive scripting. WARNING password will be visible in command history")
  private val password: String = null

  @GogoOption(name = "--ask-password", description = "Prompt new system password.")
  private val askPassword: Boolean = false

  @GogoOption(name = "--old", description = "Migrate database schema (rather than clearing then writing)")
  private val useSquerylReset = false

  @GogoOption(name = "--hard", description = "Clear the database first.")
  private val clearFirst = false

  override val requiresLogin = false

  override def doCommand(): Unit = {

    val systemPassword = Option(password) match {
      case Some(pass) =>
        System.out.println("WARNING: Password will be visible in karaf command history!")
        pass.trim
      case None =>
        val userSettings = new UserSettings(OsgiConfigReader.load(getBundleContext, "org.totalgrid.reef.user"))
        if (userSettings.getUserName == "system" && !askPassword) {
          System.out.println("Using System Password from etc/org.totalgrid.reef.user.cfg file.\nUse -p or --ask-password to manually provide password.")
          userSettings.getUserPassword
        } else {
          val stdIn = new BufferedReader(new InputStreamReader(System.in))
          System.out.println("Enter New System Password: ")
          val p1 = stdIn.readLine.trim
          System.out.println("Repeat System Password: ")
          val p2 = stdIn.readLine.trim
          if (p1 != p2) throw new Exception("Passwords do not match, please try again.")
          p2
        }
    }

    val sql = new DbInfo(OsgiConfigReader.load(getBundleContext, "org.totalgrid.reef.sql"))
    logout()

    val bundleContext = getBundleContext()

    val dbConnection = DbConnector.connect(sql, bundleContext)

    val mstore = MeasurementStoreFinder.getInstance(bundleContext)

    mstore.connect()

    try {
      try {
        CoreServicesSchema.prepareDatabase(dbConnection, clearFirst, !useSquerylReset)
      } catch {
        case ex: CoreServicesSchema.FirstMigrationNeededException =>
          println("Switching to migration based database, assuming --hard onetime only")
          CoreServicesSchema.prepareDatabase(dbConnection, true, !useSquerylReset)
      }
      if (clearFirst) println("Cleared and updated jvm database")
      else println("Updated database")
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
