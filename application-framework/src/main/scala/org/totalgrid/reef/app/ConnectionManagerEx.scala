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
package org.totalgrid.reef.app

import org.totalgrid.reef.util.{ Logging, Cancelable, Timer }
import org.totalgrid.reef.japi.client.ConnectionListener
import org.totalgrid.reef.broker.BrokerConnectionInfo
import org.totalgrid.reef.executor.{ ReactActorExecutor, Lifecycle }
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory

/**
 * handles the connection to an amqp broker, passing the valid and created Connection to the
 * applicationCreator function
 */
class ConnectionManagerEx(amqpSettings: BrokerConnectionInfo)
    extends ConnectionListener
    with Lifecycle
    with Logging {

  private val remoteFactory: AMQPSyncFactory = new AMQPSyncFactory with ReactActorExecutor {
    val broker = new QpidBrokerConnection(amqpSettings)
  }
  private val exe = new ReactActorExecutor {}

  private var isConnected = false
  private var consumers = Map.empty[ConnectionConsumer, Option[Cancelable]]
  private var delays = Map.empty[ConnectionConsumer, Timer]

  remoteFactory.addConnectionListener(this)

  def addConsumer(generator: ConnectionConsumer) = this.synchronized {
    if (consumers.get(generator).isDefined) throw new IllegalArgumentException("Consumer already added")
    val cancelable = if (isConnected) Some(generator.newConnection(remoteFactory))
    else None

    consumers += generator -> cancelable
  }
  def removeConsumer(generator: ConnectionConsumer) = this.synchronized {
    if (consumers.get(generator).isEmpty) throw new IllegalArgumentException("Unknown consumer")
    doBrokerConnectionLost(generator)
    consumers -= generator
  }

  override def onConnectionOpened() = this.synchronized {
    isConnected = true
    cancelDelays()
    consumers.keys.foreach { doBrokerConnectionStarted(_) }
  }

  override def onConnectionClosed(expected: Boolean) = this.synchronized {
    isConnected = false
    cancelDelays()
    if (!expected) {
      logger.warn("Connection to broker " + amqpSettings + " lost.")
      consumers.map { case (generator, cancelable) => doBrokerConnectionLost(generator) }
      // try reconnecting
      afterStart()
    }
  }

  private def cancelDelays() {
    delays.values.foreach { _.cancel }
    delays = Map.empty[ConnectionConsumer, Timer]
  }

  private def doBrokerConnectionStarted(generator: ConnectionConsumer): Unit = this.synchronized {
    if (isConnected) consumers.foreach {
      case (generator, cancelable) =>
        if (cancelable.isDefined) {
          logger.error("Still have an active application instance onConnectionOpened!")
          doBrokerConnectionLost(generator)
        }

        val application = tryC("Couldn't start consumer successfully: ") {
          generator.newConnection(remoteFactory)
        }
        consumers += generator -> application

        if (cancelable.isEmpty) {
          delays += generator -> exe.delay(5000) { doBrokerConnectionStarted(generator) }
        }
    }
  }

  private def doBrokerConnectionLost(generator: ConnectionConsumer) = this.synchronized {
    tryC("Couldn't stop application: ") {
      consumers.get(generator).foreach { _.foreach { _.cancel } }
    }
    consumers += generator -> None
  }

  override def afterStart() = this.synchronized {
    exe.start()
    tryC("Couldn't connect to " + amqpSettings + ": ") {
      remoteFactory.connect(3000)
    }
  }

  override def beforeStop() = this.synchronized {
    consumers.keys.foreach { doBrokerConnectionLost(_) }
    tryC("Couldn't disconnect from broker: ") {
      remoteFactory.disconnect(3000)
    }
    exe.stop()
  }

  private def tryC[A](message: => String)(func: => A): Option[A] = {
    try {
      Some(func)
    } catch {
      case error: Exception =>
        logger.warn(message + error.getMessage, error)
        None
    }
  }
}