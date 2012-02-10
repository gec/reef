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

object CoreServicesSchema {
  def prepareDatabase(dbConnection: DbConnection, clearFirst: Boolean = true, useMigrations: Boolean = false) {
    if (!useMigrations) {
      if (!clearFirst) {
        throw new IllegalArgumentException("Can't prepareDatabase without clearing data if not using migrations")
      }
      dbConnection.transaction {
        ApplicationSchema.reset()
      }
    } else {
      useDb(dbConnection) { database =>

        if (clearFirst) database.dropDatabaseObjects(null)

        upgradeDatabase(database)
      }
    }
  }

  val SCHEMA_FILE_NAME = "services-db-schema.xml"
  val SCHEMA_CONTEXT = "original"

  def upgradeDatabase(database: Database) {
    val resources = new ClassLoaderResourceAccessor(this.getClass.getClassLoader)
    val l = new Liquibase(SCHEMA_FILE_NAME, resources, database)

    l.update(SCHEMA_CONTEXT)
  }

  def useDb(dbConnection: DbConnection)(fun: (Database) => Unit) {
    dbConnection.underlyingConnection { jdbc1 =>

      val connection = new JdbcConnection(jdbc1)
      val databaseFactory: DatabaseFactory = DatabaseFactory.getInstance

      val database = databaseFactory.findCorrectDatabaseImplementation(connection)

      fun(database)
    }
  }
}