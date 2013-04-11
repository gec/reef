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
package org.totalgrid.reef.client.factory.impl

import net.agileautomata.executor4s._
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.util.IdempotentLifecycle
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.broker.{ BrokerConnectionFactory, BrokerConnectionListener, BrokerConnection }

class DefaultReconnectingFactory(factory: BrokerConnectionFactory, exe: Executor, startDelay: Long, maxDelay: Long)
    extends BrokerConnectionListener
    with IdempotentLifecycle
    with Logging {

  private var broker = Option.empty[BrokerConnection]
  private var reconnectDelay = Option.empty[Timer]

  private var watchers = Set.empty[ScalaConnectionWatcher]

  def addConnectionWatcher(watcher: ScalaConnectionWatcher) {
    this.synchronized {
      watchers += watcher
    }
  }

  def removeConnectionWatcher(watcher: ScalaConnectionWatcher) {
    this.synchronized {
      watchers -= watcher
    }
  }

  override def afterStart() {
    this.synchronized {
      logger.info("Starting Persistent Connection")
      scheduleReconnect(0, startDelay)
    }
  }

  override def beforeStop() {
    // can't hold lock while canceling because it blocks until wither we connect or the connection fails
    this.synchronized(reconnectDelay).foreach {
      _.cancel()
    }
    // if we're connected call the disconnect function, otherwise inform the watchers of failure
    this.synchronized(broker) match {
      case Some(c) => c.disconnect()
      case None => this.synchronized(watchers).foreach {
        _.onConnectionClosed(true)
      }
    }
  }

  private def tryConnection(nextDelay: Long) {
    try {
      logger.info("Connecting to broker")
      val connection = factory.connect
      logger.info("Connected to broker")

      this.synchronized {
        broker = Some(connection)
        connection.addListener(this)
        watchers.foreach {
          _.onConnectionOpened(broker.get)
        }
      }
    } catch {
      case ex: ReefServiceException =>
        logger.info("Error connecting to broker: " + ex.getMessage, ex)
        logger.info("Delaying reconnect: " + nextDelay * 2)
        this.synchronized {
          scheduleReconnect(nextDelay, nextDelay * 2)
        }
    }
  }

  def onDisconnect(expected: Boolean) {
    this.synchronized {
      logger.info("Disconnected from broker, expected: " + expected)
      broker.foreach(_.removeListener(this))
      broker = None
      watchers.foreach {
        _.onConnectionClosed(expected)
      }
      if (!expected) {
        logger.warn("Unexpected disconnection. Attempting reconnect.")
        scheduleReconnect(0, startDelay)
      }
    }
  }

  private def scheduleReconnect(delay: Long, nextDelay: Long) {
    reconnectDelay = Some(exe.schedule(delay.milliseconds) {
      tryConnection(nextDelay.min(maxDelay))
    })
  }
}
