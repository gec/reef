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
import org.apache.qpid.transport.{ ConnectionSettings, Connection }
import org.totalgrid.reef.client.settings.AmqpSettings
import java.io.File
import org.totalgrid.reef.client.exception.ServiceIOException
import org.apache.qpid.client.transport.ClientConnectionDelegate
import org.apache.qpid.jms.{ BrokerDetails, ConnectionURL }
import org.apache.qpid.framing.AMQShortString
import java.util

object QpidBrokerConnectionFactory {

  def loadssl(config: AmqpSettings, qpidSettings: ConnectionSettings) {
    if (config.getSsl) {
      if (config.getTrustStore == null || config.getTrustStore == "") {
        throw new IllegalArgumentException("ssl is enabled, trustStore must be not null and not empty")
      }
      if (config.getTrustStorePassword == null || config.getTrustStorePassword == "") {
        throw new IllegalArgumentException("ssl is enabled, trustStorePassword must be not null and not empty")
      }

      val trustStoreFile = new File(config.getTrustStore)
      if (!trustStoreFile.canRead) throw new ServiceIOException("Cannot access trustStore file: " + trustStoreFile.getAbsolutePath)

      val (keyStore, keyStorePassword) = if (config.getKeyStore != null && config.getKeyStore != "") {
        val keyStoreFile = new File(config.getKeyStore)
        if (!keyStoreFile.canRead) throw new ServiceIOException("Cannot access keyStore file: " + trustStoreFile.getAbsolutePath)
        (config.getKeyStore, config.getKeyStorePassword)
      } else {
        (config.getTrustStore, config.getTrustStorePassword)
      }

      qpidSettings.setTrustStorePath(config.getTrustStore)
      qpidSettings.setTrustStorePassword(config.getTrustStorePassword)
      qpidSettings.setKeyStorePath(keyStore)
      qpidSettings.setKeyStorePassword(keyStorePassword)
    }
  }

  // This is a hack to not have to rewrite the entire ClientConnectionDelegate
  class SimpleUrl(user: String, pass: String) extends ConnectionURL {
    def getURL: String = ""
    def getFailoverMethod: String = ""
    def getFailoverOption(key: String): String = ""
    def getBrokerCount: Int = 0
    def getBrokerDetails(index: Int): BrokerDetails = null
    def addBrokerDetails(broker: BrokerDetails) {}
    def setBrokerDetails(brokers: util.List[BrokerDetails]) {}
    def getAllBrokerDetails: util.List[BrokerDetails] = null
    def getClientName: String = ""
    def setClientName(clientName: String) {}
    def getUsername: String = user
    def setUsername(username: String) {}
    def getPassword: String = pass
    def setPassword(password: String) {}
    def getVirtualHost: String = ""
    def setVirtualHost(virtualHost: String) {}
    def getOption(key: String): String = ""
    def setOption(key: String, value: String) {}
    def getDefaultQueueExchangeName: AMQShortString = null
    def getDefaultTopicExchangeName: AMQShortString = null
    def getTemporaryQueueExchangeName: AMQShortString = null
    def getTemporaryTopicExchangeName: AMQShortString = null
  }

}

class QpidBrokerConnectionFactory(config: AmqpSettings) extends BrokerConnectionFactory {

  private def makeSettings = {
    // need to manually create the ConnectionSettings object because there is no overload that
    // includes the heartbeatTime setting
    val settings = new ConnectionSettings()
    settings.setHost(config.getHost)
    settings.setPort(config.getPort)
    settings.setVhost(config.getVirtualHost)
    settings.setUsername(config.getUser)
    settings.setPassword(config.getPassword)
    settings.setUseSSL(config.getSsl)
    // TODO: qpid 0.14 check if setting setSaslMechs is still necessary
    settings.setSaslMechs("PLAIN")
    settings.setHeartbeatInterval(config.getHeartbeatTimeSeconds)
    settings
  }

  def connect: BrokerConnection = {
    try {
      val settings = makeSettings
      QpidBrokerConnectionFactory.loadssl(config, settings)
      val conn = new Connection
      conn.setConnectionDelegate(new ClientConnectionDelegate(settings, new QpidBrokerConnectionFactory.SimpleUrl(settings.getUsername, settings.getPassword)))
      val broker = new QpidBrokerConnection(conn, config.getTtlMilliseconds())
      conn.connect(settings)
      broker
    } catch {
      case ex: Exception =>
        throw new ServiceIOException("Cannot connect to broker: " + config.toString + " - " + ex.getMessage, ex)
    }
  }

  override def toString() = config.toString()
}