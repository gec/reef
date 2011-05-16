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
package org.totalgrid.reef.persistence.squeryl.postgresql

object PostgresqlReset {
  /**
   * HACK to remove all tables from postgres database to workaround table name changes between version
   * 0.9.4-RC3 and 0.9.4-RC6 of squeryl. This will be mooted when we move to a proper database schema migration system.
   * Also mooted when we EOL 0.2.3
   * TODO: remove workaroundToClearAllTablesOnPostgresql
   */
  def reset() {

    import org.squeryl.Session
    import org.squeryl.PrimitiveTypeMode._
    transaction {
      val s = Session.currentSession.connection.createStatement
      val rs = s.executeQuery("SELECT 'DROP TABLE \"' || c.relname || '\" CASCADE;' FROM pg_catalog.pg_class AS c LEFT JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace WHERE relkind ='r' AND n.nspname NOT IN ('pg_catalog', 'pg_toast') AND pg_catalog.pg_table_is_visible(c.oid);")
      val dropStatement = Session.currentSession.connection.createStatement
      while (rs.next()) {
        dropStatement.addBatch(rs.getString(1))
      }
      dropStatement.executeBatch()
      s.close
      rs.close
      dropStatement.close
    }
  }
}