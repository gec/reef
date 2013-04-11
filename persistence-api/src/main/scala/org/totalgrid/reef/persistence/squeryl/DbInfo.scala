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

import java.util.Dictionary
import org.totalgrid.reef.client.settings.util.{ PropertyReader, PropertyLoading }
import com.typesafe.scalalogging.slf4j.Logging

object DbInfo extends Logging {

  def loadInfo(fileName: String): DbInfo = new DbInfo(PropertyReader.readFromFile(fileName))

  def getUrl(props: Dictionary[Object, Object]) = {
    // TODO: in 0.5.0 we can remove this shim code
    val url = PropertyLoading.getString("org.totalgrid.reef.sql.url", props, "")
    if (url == "") {
      val typ = PropertyLoading.getString("org.totalgrid.reef.sql.type", props)
      val host = PropertyLoading.getString("org.totalgrid.reef.sql.host", props)
      val port = PropertyLoading.getInt("org.totalgrid.reef.sql.port", props)
      val database = PropertyLoading.getString("org.totalgrid.reef.sql.database", props)
      val createdUrl = "jdbc:%s://%s:%s/%s".format(typ, host, port, database)
      logger.warn("DEPRECATION WARNING: org.totalgrid.reef.sql.url not in config file, generated url from deprecated type/host/port/database: " + createdUrl)
      createdUrl
    } else {
      url
    }
  }
}

case class DbInfo(dbType: String, url: String, user: String, password: String,
    slowQueryTimeMilli: Long, poolMaxActive: Int) {

  def this(props: Dictionary[Object, Object]) = this(
    PropertyLoading.getString("org.totalgrid.reef.sql.type", props),
    DbInfo.getUrl(props),
    PropertyLoading.getString("org.totalgrid.reef.sql.user", props),
    PropertyLoading.getString("org.totalgrid.reef.sql.password", props),
    PropertyLoading.getInt("org.totalgrid.reef.sql.slowquery", props),
    PropertyLoading.getInt("org.totalgrid.reef.sql.pool.maxactive", props, 8))

  // custom too string to hide password
  override def toString() = user + "@" + url
}
