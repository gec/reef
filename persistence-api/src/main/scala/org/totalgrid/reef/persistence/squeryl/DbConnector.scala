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

import com.typesafe.scalalogging.slf4j.Logging
import org.osgi.framework.BundleContext
import org.totalgrid.reef.osgi.Helpers._
import java.lang.Exception

object DbConnector {

  private val connectedAdapters = scala.collection.mutable.Map.empty[String, DbConnector]

  def connect(dbInfo: DbInfo, context: BundleContext): DbConnection = connectedAdapters.synchronized {
    val connector = connectedAdapters.get(dbInfo.dbType) match {
      case None =>
        val services = context.getServiceReferences(classOf[DbConnector].getName, "(org.totalgrid.reef.sql.type=" + dbInfo.dbType + ")")

        services.headOption match {
          case Some(srvRef) =>
            val connector = context.getService(srvRef).asInstanceOf[DbConnector]
            connectedAdapters.put(dbInfo.dbType, connector)
            connector
          case None => throw new Exception("No connector found for " + dbInfo.dbType)
        }
      case Some(c) => c
    }
    connector.connect(dbInfo)

  }

  def connect(dbInfo: DbInfo): DbConnection = connectedAdapters.synchronized {
    val connector = connectedAdapters.get(dbInfo.dbType) match {
      case None =>
        val klass = Class.forName("org.totalgrid.reef.persistence.squeryl." + dbInfo.dbType + ".Connector")
        val connector = klass.newInstance.asInstanceOf[DbConnector]
        connectedAdapters.put(dbInfo.dbType, connector)
        connector
      case Some(c) => c
    }
    connector.connect(dbInfo)
  }
}

trait DbConnector {
  def connect(dbInfo: DbInfo): DbConnection
}

abstract class DbConnectorBase extends DbConnector with Logging {
  private var connections = Map.empty[DbInfo, DbConnection]

  protected def _connect(dbInfo: DbInfo): DbConnection

  def connect(dbInfo: DbInfo): DbConnection = {
    try {
      connections.get(dbInfo) match {
        case Some(c) => c
        case None =>
          val c = _connect(dbInfo)
          connections += dbInfo -> c
          c
      }
    } catch {
      case e: Exception =>
        logger.error("got exception trying to connect to database: " + dbInfo.toString, e)
        throw e
    }
  }
}

