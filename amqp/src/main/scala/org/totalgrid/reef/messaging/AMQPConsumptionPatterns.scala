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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.sapi.RoutingKey

import org.totalgrid.reef.broker.api._

/**
 * base class for AMQP subscripton modes, provides online/offline notifiers
 */
trait AMQPConsumptionPattern extends ChannelObserver with ObserverableBrokerObject with BrokerChannelCloseListener with CloseableChannel {

  def getQueue(broker: BrokerChannel): String

  /// store the queue name we registered with so we can do the resubscribe call correctly
  var queue: Option[String] = None
  var channel: Option[BrokerChannel] = None

  def online(broker: BrokerChannel) = this.synchronized {
    broker.addCloseListener(this)
    channel = Some(broker)
    queue = Some(getQueue(broker))
    onConnectEvent(true)
    onChange(true, queue.get)
  }

  def onClosed(broker: BrokerChannel, expected: Boolean) = this.synchronized {
    broker.removeCloseListener(this)
    onConnectEvent(false)
    onChange(false, "")
    queue = None
    channel = None
  }

  def close() = this.synchronized { channel.foreach(_.close()) }
}

object QueuePatterns {

  /**
   *  Returns a non-exclusive queue name bound to a topic exchange
   */
  def getCompetingConsumer(broker: BrokerChannel, exchange: String, queueName: String, routingKey: String, consumer: MessageConsumer): String = {
    val queue = broker.declareQueue(queueName, false, false)
    assert(queue == queueName)
    broker.declareExchange(exchange)
    broker.bindQueue(queueName, exchange, routingKey)
    broker.listen(queueName, consumer)
    queueName
  }

  /**
   *  Listen to a queue that is already bound to an exchange
   */
  def getPreparedQueue(broker: BrokerChannel, queueName: String, consumer: MessageConsumer): String = {
    val queue = broker.declareQueue(queueName, false, false)
    assert(queue == queueName)
    broker.listen(queueName, consumer)
    queueName
  }

  /**
   * Create an exclusive private queue on the message broker, that has a pre-configured event acceptor
   */
  def getPrivateUnboundQueue(broker: BrokerChannel, consumer: MessageConsumer): String = {
    val queue = broker.declareQueue("*", true, true)
    broker.listen(queue, consumer)
    queue
  }

  /**
   * Create an exclusive private queue on the message broker with no event acceptor
   */
  def getLateBoundPrivateUnboundQueue(broker: BrokerChannel): String = {
    val queue = broker.declareQueue("*", true, true)
    queue
  }

  /**
   * Create an exculsive private queue on the message broker, and bind to an exchange with a specific key
   */
  def getExclusiveQueue(broker: BrokerChannel, exchange: String, routingKey: String, consumer: MessageConsumer): String = {
    val queue = broker.declareQueue("*", true, true)
    broker.declareExchange(exchange)
    broker.bindQueue(queue, exchange, routingKey)
    broker.listen(queue, consumer)
    queue
  }

  /**
   * Create an exclusive private queue on the broker, and bind to an exchange with the name of the queue
   */
  def getPrivateResponseQueue(broker: BrokerChannel, exchange: String, consumer: MessageConsumer): String = {
    val queue = broker.declareQueue("*", true, true)
    broker.declareExchange(exchange)
    broker.bindQueue(queue, exchange, queue)
    broker.listen(queue, consumer)
    queue
  }

}

/**
 * Listen to a shared queue so many consumers can service the same requests.  Queues are declared without
 * exclusive and autodelete options.
 */
class AMQPCompetingConsumer(exchange: String, queueName: String, routingKey: RoutingKey, consumer: MessageConsumer) extends AMQPConsumptionPattern {
  override def getQueue(broker: BrokerChannel): String =
    QueuePatterns.getCompetingConsumer(broker, exchange, queueName, routingKey.key, consumer)
}

/**
 * Listen to a preprepared queue, no checking is done to make sure it exists
 */
class AMQPExternallyPreparedQueueListener(queueName: String, consumer: MessageConsumer) extends AMQPConsumptionPattern {
  override def getQueue(broker: BrokerChannel): String =
    QueuePatterns.getPreparedQueue(broker, queueName, consumer)
}

/**
 * Create an exculsive private queue on the message broker, it will need to be bound later.
 */
class AMQPUnboundPrivateQueueListener(consumer: MessageConsumer) extends AMQPConsumptionPattern {
  override def getQueue(broker: BrokerChannel): String =
    QueuePatterns.getPrivateUnboundQueue(broker, consumer)
}

/**
 * Create an exculsive private queue on the message broker, it will need to be bound later.
 */
class AMQPPrivateResponseQueueListener(exchange: String, consumer: MessageConsumer) extends AMQPConsumptionPattern {
  override def getQueue(broker: BrokerChannel): String =
    QueuePatterns.getPrivateResponseQueue(broker, exchange, consumer)
}

/**
 * Listen to a private queue bound to the exchange, allows duplication of messages, not to be used for big S services.
 */
class AMQPExclusiveConsumer(exchange: String, routingKey: RoutingKey, consumer: MessageConsumer) extends AMQPConsumptionPattern {
  override def getQueue(broker: BrokerChannel): String =
    QueuePatterns.getExclusiveQueue(broker, exchange, routingKey.key, consumer)
}
