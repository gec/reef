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
package org.totalgrid.reef.persistence.squeryl.postgresql

import org.squeryl.Session
import org.squeryl.adapters.PostgreSqlAdapter

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.persistence.squeryl._

class Connector extends DbConnectorBase with Logging {

  protected def _connect(dbInfo: DbInfo) = {

    if (dbInfo.dbType != "postgresql") {
      throw new IllegalArgumentException("Trying to use postgresql adapter to talk to database with type: " + dbInfo.dbType)
    }

    val pool = new org.apache.commons.dbcp.BasicDataSource
    pool.setDriverClassName("org.postgresql.Driver")
    pool.setUrl(dbInfo.url)
    pool.setUsername(dbInfo.user)
    pool.setPassword(dbInfo.password)
    pool.setMaxActive(dbInfo.poolMaxActive)

    logger.info("Connecting to Database: " + dbInfo.url)

    val sessionFactory = () => {
      Session.create(
        pool.getConnection(),
        new PostgreSqlAdapter with SlowQueryTracing {
          val slowQueryTimeMilli = dbInfo.slowQueryTimeMilli
        })
    }

    new SessionDbConnection(sessionFactory)
  }
}