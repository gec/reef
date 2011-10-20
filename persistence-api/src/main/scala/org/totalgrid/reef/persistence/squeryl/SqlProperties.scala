/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.persistence.squeryl

import org.totalgrid.reef.api.sapi.config.ConfigReader

object SqlProperties {

  def get(cr: ConfigReader): DbInfo = {
    val dbtype = cr.getString("org.totalgrid.reef.sql.type", "postgresql")
    val host = cr.getString("org.totalgrid.reef.sql.host", "127.0.0.1")
    val port = cr.getInt("org.totalgrid.reef.sql.port", 5432)
    val db = cr.getString("org.totalgrid.reef.sql.database", "reef_d")
    val user = cr.getString("org.totalgrid.reef.sql.user", "core")
    val pass = cr.getString("org.totalgrid.reef.sql.password", "core")
    val slowQueryMs = cr.getInt("org.totalgrid.reef.sql.slowquery", 100)

    DbInfo(dbtype, host, port, db, user, pass, slowQueryMs)
  }

}
