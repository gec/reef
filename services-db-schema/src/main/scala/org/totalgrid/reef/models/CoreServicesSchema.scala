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
package org.totalgrid.reef.models

import org.totalgrid.reef.persistence.squeryl.DbConnection
import liquibase.database.jvm.JdbcConnection
import liquibase.Liquibase
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.database.{ Database, DatabaseFactory }
import liquibase.logging.LogFactory

object CoreServicesSchema {

  // if we are starting migrations we need to make sure that the user knows the database is going to get
  // reset
  class FirstMigrationNeededException extends Exception("Can't start migrating database without clearing data first (rerun with --hard)")
  class MigrationNeededException extends Exception("Database has changed, cannot start services without running resetdb.")

  def prepareDatabase(dbConnection: DbConnection, clearFirst: Boolean = true, useMigrations: Boolean = false) {
    if (!useMigrations) {
      if (!clearFirst) {
        throw new IllegalArgumentException("Can't prepareDatabase without clearing data if not using migrations")
      }
      useDb(dbConnection) { clearDatabase(_) }

      dbConnection.transaction {
        ApplicationSchema.reset()
      }
    } else {
      useDb(dbConnection) { database =>

        if (clearFirst) clearDatabase(database)

        upgradeDatabase(database, clearFirst)
      }
    }
  }

  def checkDatabase(dbConnection: DbConnection) {

    useDb(dbConnection) { database =>
      val resources = new ClassLoaderResourceAccessor(this.getClass.getClassLoader)
      val l = new Liquibase(SCHEMA_FILE_NAME, resources, database)

      import scala.collection.JavaConversions._
      val unrun = l.listUnrunChangeSets(SCHEMA_CONTEXT).toList
      if (!unrun.isEmpty) throw new MigrationNeededException()
    }
  }

  def clearDatabase(database: Database) {
    // dropDatabaseObjects doesn't delete the lock table so we change the names
    // do the delete and then change them back to make sure it gets correctly cleared out
    val lockName = database.getDatabaseChangeLogLockTableName
    val logName = database.getDatabaseChangeLogTableName
    database.setDatabaseChangeLogLockTableName(lockName + "_temp")
    database.setDatabaseChangeLogTableName(logName + "_temp")
    database.dropDatabaseObjects(null)
    database.setDatabaseChangeLogLockTableName(lockName)
    database.setDatabaseChangeLogTableName(logName)
    if (database.hasDatabaseChangeLogTable || database.hasDatabaseChangeLogLockTable) {
      throw new Exception("Dropping db objects doesn't include change log tables")
    }
  }

  val SCHEMA_FILE_NAME = "services-db-schema.xml"
  val SCHEMA_CONTEXT = "original"

  def upgradeDatabase(database: Database, clearFirst: Boolean) {
    val resources = new ClassLoaderResourceAccessor(this.getClass.getClassLoader)
    val l = new Liquibase(SCHEMA_FILE_NAME, resources, database)

    if (!clearFirst) {
      import scala.collection.JavaConversions._
      val unrun = l.listUnrunChangeSets(SCHEMA_CONTEXT).toList
      if (!unrun.isEmpty && unrun.head.getId == "1327943117559-1") {
        throw new FirstMigrationNeededException()
      }
    }

    l.update(SCHEMA_CONTEXT)
  }

  def useDb(dbConnection: DbConnection)(fun: (Database) => Unit) {
    // surpress the logging to stderr
    LogFactory.setLoggingLevel("warning")
    System.setProperty("liquibase.defaultlogger.level", "warning")

    dbConnection.underlyingConnection { jdbc1 =>

      val connection = new JdbcConnection(jdbc1)
      val databaseFactory: DatabaseFactory = DatabaseFactory.getInstance

      val database = databaseFactory.findCorrectDatabaseImplementation(connection)

      fun(database)
    }
  }
}