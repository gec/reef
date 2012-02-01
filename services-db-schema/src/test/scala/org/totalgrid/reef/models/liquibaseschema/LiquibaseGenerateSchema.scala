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
package org.totalgrid.reef.models.liquibaseschema

import liquibase.diff.Diff
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatest.FunSuite
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import liquibase.database.Database
import java.io.File
import liquibase.Liquibase
import liquibase.resource.FileSystemResourceAccessor
import org.totalgrid.reef.models._
import org.totalgrid.reef.measurementstore.squeryl.SqlMeasurementStoreSchema

@RunWith(classOf[JUnitRunner])
class LiquibaseGenerateSchema extends FunSuite {

  import CoreServicesSchema._

  test("Changelog output") {

    val baseInfo = DbInfo.loadInfo("../org.totalgrid.reef.test.cfg")

    val testInfo = baseInfo.copy(url = baseInfo.url.replace("reef_t", "reef2_t"))

    println("Connecting to standard test database " + baseInfo.url)
    val dbConnection = DbConnector.connect(baseInfo)

    println("Connecting to target database " + testInfo.url)
    val dbConnection2 = DbConnector.connect(testInfo)

    println("Generating standard squeryl schema in standard database")
    dbConnection.transaction {
      ApplicationSchema.reset()
      SqlMeasurementStoreSchema.reset()
    }

    useDb(dbConnection) { referenceDb =>

      //makeInitialChangeSet("src/main/resources/test-changelog2.xml", referenceDb)

      useDb(dbConnection2) { targetDb =>

        println("Clearing target database")
        targetDb.dropDatabaseObjects(null)

        println("Running current migrations")
        upgradeDatabase(targetDb)

        println("Checking databases have same schema")
        if (doDiff(referenceDb, targetDb)) {

          // make a tempfile for the incremental file
          val incremental = File.createTempFile("incrementalChanges", ".xml")
          incremental.delete()

          println("Schema has changed, generating additional changesets")
          updateChangeLog("src/main/resources/" + SCHEMA_FILE_NAME, incremental.getPath, referenceDb, targetDb)

          println("Running upgrade from where previous migrations ended")
          val resources = new FileSystemResourceAccessor()
          val l = new Liquibase(incremental.getPath, resources, targetDb)
          l.update(SCHEMA_CONTEXT)

          if (doDiff(referenceDb, targetDb)) {
            fail("Updated changes set file still doesn't migrate to match schema!")
          }

          fail("Schema updated!, re-run tests to verify migrations")
        } else {
          println("Database match!")
        }
      }
    }
  }

  /**
   * calculate the incremental changes to targetDb to get it to match reference Db. Add those changes to the main
   * file and a temporary file (so we can run just incremental to verify its working).
   */
  private def updateChangeLog(realChangeLog: String, tempChangeLog: String, referenceDatabase: Database, targetDatabase: Database) {
    val diff = new Diff(referenceDatabase, targetDatabase)
    setDiffLogger(diff)

    val diffResult = diff.compare
    diffResult.setChangeSetAuthor("incremental")
    diffResult.setChangeSetContext(SCHEMA_CONTEXT)

    diffResult.printChangeLog(realChangeLog, targetDatabase)
    diffResult.printChangeLog(tempChangeLog, targetDatabase)
  }

  /**
   * check to see if there is a difference between the two databases (indicated schema changed)
   */
  private def doDiff(referenceDatabase: Database, targetDatabase: Database) = {
    val diff = new Diff(referenceDatabase, targetDatabase)
    setDiffLogger(diff)

    val diffResult = diff.compare

    val differenceFound = diffResult.differencesFound()

    if (differenceFound) diffResult.printResult(System.out)

    differenceFound
  }

  /**
   * code to generate a new changeset from scratch, we should always be updating the current
   * changeset file. We only need to re generate the initial set if making a clean break in the future. (0.5.0?)
   *
   * When generating the initial files (or adding tables) there is a bug in liquibaseschema where it breakes
   * the primary key names, they need to manually fixed to have the correct casing.
   */
  private def makeInitialChangeSet(changeLog: String, database: Database) {
    val diff = new Diff(database, null: String)
    setDiffLogger(diff)

    val diffResult = diff.compare

    diffResult.setChangeSetAuthor("diff")
    diffResult.setChangeSetContext(SCHEMA_CONTEXT)

    new File(changeLog).delete()
    diffResult.printChangeLog(changeLog, database)
  }

  private def setDiffLogger(diff: Diff) {
    //    import liquibaseschema.diff.DiffStatusListener
    //    diff.addStatusListener(new DiffStatusListener{
    //      def statusUpdate(message: String) {println(message)}
    //    })
  }
}