/**
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.persistence.squeryl

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.persistence.AsyncBufferReactor
import org.totalgrid.reef.persistence.ConnectionReactor.Observer
import org.totalgrid.reef.reactor.Reactable

object DbConnector {
  def connect(dbInfo: DbInfo): Option[Boolean] = {
    val klass = Class.forName("org.totalgrid.reef.persistence.squeryl." + dbInfo.dbType + ".Connector")
    val connector = klass.newInstance.asInstanceOf[DbConnectorBase]
    connector.connect(dbInfo)
  }
}

abstract class DbConnectorBase extends Logging {
  private var connected = false

  def _connect(dbInfo: DbInfo)

  def connect(dbInfo: DbInfo): Option[Boolean] = {
    try {
      if (!connected) {
        _connect(dbInfo)
        connected = true
      }
      Some(connected)
    } catch {
      case e: Exception =>
        error("got exception trying to connect to database, is it correctly configured?")
        error("try granting access to db:")
        error("grant all on %s.* to '%s'@'%%' identified by '%s';".format(dbInfo.database, dbInfo.user, dbInfo.password))
        error("grant all on %s.* to '%s'@'localhost' identified by '%s';".format(dbInfo.database, dbInfo.user, dbInfo.password))
        error("create database %s;".format(dbInfo.database))
        error("Exception connecting", e)
        throw e
    }
  }
}

class SimpleDbConnection(connInfo: DbInfo, reactor: Reactable)(obs: Observer)
    extends AsyncBufferReactor[Boolean](reactor, obs) {

  override def connectFun() = DbConnector.connect(connInfo)
}
