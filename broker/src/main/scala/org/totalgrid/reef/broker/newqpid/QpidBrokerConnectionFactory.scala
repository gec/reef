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
package org.totalgrid.reef.broker.newqpid

import org.totalgrid.reef.broker.newapi._
import org.totalgrid.reef.broker.api.BrokerConnectionInfo
import org.apache.qpid.transport.Connection

object QpidBrokerConnectionFactory {

  def loadssl(config: BrokerConnectionInfo) {
    if (config.ssl) {
      if (config.trustStore == null || config.trustStore == "") {
        throw new IllegalArgumentException("ssl is enabled, trustStore must be not null and not empty: " + config.trustStore)
      }
      if (config.trustStorePassword == null || config.trustStorePassword == "") {
        throw new IllegalArgumentException("ssl is enabled, trustStorePassword must be not null and not empty")
      }

      System.setProperty("javax.net.ssl.trustStore", config.trustStore)
      System.setProperty("javax.net.ssl.trustStorePassword", config.trustStorePassword)

      System.setProperty("javax.net.ssl.keyStore", if (config.keyStore == "") config.trustStore else config.keyStore)
      System.setProperty("javax.net.ssl.keyStorePassword", if (config.keyStore == "") config.trustStorePassword else config.keyStorePassword)
    }
  }

}

class QpidBrokerConnectionFactory(config: BrokerConnectionInfo) extends BrokerConnectionFactory {

  override def toString() = config.toString

  def connect: BrokerConnection = {
    QpidBrokerConnectionFactory.loadssl(config)
    val conn = new Connection
    val broker = new QpidBrokerConnection(conn)
    conn.connect(config.host, config.port, config.virtualHost, config.user, config.password, config.ssl)
    broker
  }

}