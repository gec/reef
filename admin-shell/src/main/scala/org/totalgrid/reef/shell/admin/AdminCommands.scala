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

@Command(scope = "reef", name = "resetdb", description = "Clears and resets sql tables")
class ResetDatabaseCommand extends ReefCommandSupport {

  @GogoOption(name = "-p", description = "password for non-interactive scripting. WARNING password will be visible in command history")
  private var password: String = null

  @GogoOption(name = "-m", description = "Migrate database schema (rather than clearing then writing)")
  private var useMigrations = false

  // TODO: once we believe in the migrations switch this to default to not clear
  @GogoOption(name = "-dontClear", description = "Don't clear database first")
  private var dontClearFirst = false

  override val requiresLogin = false

  override def doCommand(): Unit = {

    val systemPassword = Option(password) match {
      case Some(pass) =>
        System.out.println("WARNING: Password will be visible in karaf command history!")
        pass.trim
      case None =>
        val stdIn = new BufferedReader(new InputStreamReader(System.in))
        System.out.println("Enter New System Password: ")
        val p1 = stdIn.readLine.trim
        System.out.println("Repeat System Password: ")
        val p2 = stdIn.readLine.trim
        if (p1 != p2) throw new Exception("Passwords do not match, please try again.")
        p2
    }

    val sql = new DbInfo(OsgiConfigReader(getBundleContext, "org.totalgrid.reef.sql").getProperties)
    logout()

    val bundleContext = getBundleContext()

    val dbConnection = DbConnector.connect(sql, bundleContext)

    val mstore = MeasurementStoreFinder.getInstance(bundleContext)

    mstore.connect()

    try {
      CoreServicesSchema.prepareDatabase(dbConnection, !dontClearFirst, useMigrations)
      ServiceBootstrap.seed(dbConnection, systemPassword)
      println("Cleared and updated jvm database")

      if (mstore.reset) {
        println("Cleared measurement store")
      } else {
        println("NOTE: measurement store not reset, needs to be done manually")
      }
    } catch {
      case ex => println("Reset failed: " + ex.toString)
    }

    mstore.disconnect()
  }

}

