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
package org.totalgrid.reef.broker.qpid

import org.totalgrid.reef.broker._
import org.apache.qpid.transport.Connection
import org.totalgrid.reef.api.japi.settings.AmqpSettings

object QpidBrokerConnectionFactory {

  def loadssl(config: AmqpSettings) {
    if (config.getSsl) {
      if (config.getTrustStore == null || config.getTrustStore == "") {
        throw new IllegalArgumentException("ssl is enabled, trustStore must be not null and not empty")
      }
      if (config.getTrustStorePassword == null || config.getTrustStorePassword == "") {
        throw new IllegalArgumentException("ssl is enabled, trustStorePassword must be not null and not empty")
      }

      System.setProperty("javax.net.ssl.trustStore", config.getTrustStore)
      System.setProperty("javax.net.ssl.trustStorePassword", config.getTrustStorePassword)

      System.setProperty("javax.net.ssl.keyStore", if (config.getKeyStore == "") config.getTrustStore else config.getKeyStore)
      System.setProperty("javax.net.ssl.keyStorePassword", if (config.getKeyStore == "") config.getTrustStorePassword else config.getKeyStorePassword)
    }
  }

}

class QpidBrokerConnectionFactory(config: AmqpSettings) extends BrokerConnectionFactory {

  def connect: BrokerConnection = {
    QpidBrokerConnectionFactory.loadssl(config)
    val conn = new Connection
    val broker = new QpidBrokerConnection(conn)
    conn.connect(config.getHost, config.getPort, config.getVirtualHost, config.getUser, config.getPassword, config.getSsl)
    broker
  }

}