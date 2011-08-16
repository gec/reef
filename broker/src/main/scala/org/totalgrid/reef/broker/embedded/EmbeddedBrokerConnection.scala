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
package org.totalgrid.reef.broker.embedded

import scala.collection.immutable

import org.totalgrid.reef.broker._
import org.totalgrid.reef.japi.ServiceIOException

/**
 * Very simple round robin implementation for mocking purposes
 */
trait RoundRobinList[A] {
  var round = 0

  var list = List.empty[A]

  def next(): A = {
    val ret = list(round % list.size)
    round += 1
    ret
  }
  def add(entry: A) {
    list = entry :: list
  }
}

object EmbeddedBrokerConnection {
  def matches(routingKey: String, bindingKey: String): Boolean = {
    val r = routingKey.split('.')
    val b = bindingKey.split('.')

    // if there isn't a multi section matcher, the keys need to be the same length to match
    if (!bindingKey.contains("#") && r.size != b.size) return false

    //once we find a section with a '#' the rest of the key doesn't matter
    val removed_hashes = b.takeWhile(!_.contains("#"))
    val nh = removed_hashes.zip(r)
    nh.forall(tuple => tuple._2.matches(tuple._1.replaceAll("\\*", ".*")))
  }
}

class EmbeddedBrokerChannel(parent: EmbeddedBrokerConnection) extends BrokerChannel {
  var started = true

  var messageConsumer: Option[MessageConsumer] = None
  var queueName: Option[String] = None

  def start() {
    started = true
    parent.listenInternal(queueName.get, messageConsumer.get)
  }

  def isOpen = started

  def close() {
    val wasStarted = started
    started = false
    if (wasStarted) onClose(true)
  }

  def throwOnClosed() = if (!started) throw new ServiceIOException("Already closed")

  def listen(queue: String, mc: MessageConsumer) = {
    throwOnClosed()
    queueName = Some(queue)
    messageConsumer = Some(mc)
  }

  def publish(exchange: String, key: String, b: Array[Byte], replyTo: Option[Destination]) = {
    throwOnClosed()
    parent.publishInternal(exchange, key, b, replyTo)
  }

  def unbindQueue(queue: String, exchange: String, key: String) = {
    throwOnClosed()
    parent.unbindQueueInternal(queue, exchange, key)
  }

  def bindQueue(queue: String, exchange: String, key: String, unbindFirst: Boolean) = {
    throwOnClosed()
    parent.bindQueueInternal(queue, exchange, key, unbindFirst)
  }

  def declareExchange(exchange: String, exchangeType: String) = {
    throwOnClosed()
    parent.declareExchangeInternal(exchange, exchangeType)
  }

  def declareQueue(queue: String, autoDelete: Boolean, exclusive: Boolean) = {
    throwOnClosed()
    parent.declareQueueInternal(queue, autoDelete, exclusive)
  }
}

class EmbeddedBrokerConnection(connectCorrectly: Boolean = true, reportCorrectClosure: Boolean = true) extends BrokerConnection {
  import EmbeddedBrokerConnection._

  private var channels = List.empty[EmbeddedBrokerChannel]

  def newChannel(): BrokerChannel = {
    if (!isConnected) throw new Exception("Mock broker not connected")
    val c = new EmbeddedBrokerChannel(this)
    channels = c :: channels
    c
  }

  final override def connect(): Boolean = {
    if (!connectCorrectly) false
    else {
      this.setOpen()
      true
    }
  }

  /// nothing special is needed to shut down the mock broker, there are no circular
  /// dependencies or listeners to unravel
  final override def disconnect(): Boolean = {
    this.setClosed(true)
    channels.foreach(_.close())
    true
  }

  case class ExchangeBinding(key: String, queue: String)

  var queues = immutable.Map.empty[String, RoundRobinList[MessageConsumer]]
  var exchanges = immutable.Map.empty[String, List[ExchangeBinding]]
  var queueNum = 0

  def publishInternal(exchange: String, routingKey: String, b: Array[Byte], replyTo: Option[Destination]) = {
    // we get all the destinations before sending any messages so we are not synchronized when we make the callback
    // because that will lead to a deadlock if the callback causes another message to be sent on the same thread
    getDestinations(exchange, routingKey).foreach(mc => mc.receive(b, replyTo))
  }

  private def getDestinations(exchange: String, routingKey: String): List[MessageConsumer] = {
    synchronized {
      exchanges.get(exchange) match {
        case Some(l: List[_]) =>
          val matching: List[String] = l.filter { eb: ExchangeBinding => matches(routingKey, eb.key) }.map { eb: ExchangeBinding => eb.queue }
          matching.map { queue: String =>
            //println("publishing: " + exchange + " + " + routingKey + " => " + queue)
            queues.get(queue) match {
              case Some(roundRobin) =>
                Some(roundRobin.next())
              case None =>
                None
            }
          }.flatten
        case None => throw new Exception("undefined exchange")
      }
    }
  }

  def listenInternal(queue: String, mc: MessageConsumer) {
    synchronized {
      queues.get(queue) match {
        case Some(roundRobin) =>
          //println("added "+mc+" as listener on " + queue)
          roundRobin.add(mc)
        case None =>
          queues = queues + (queue -> new RoundRobinList[MessageConsumer] {})
          listenInternal(queue, mc)
      }
    }
  }

  private def privateName(queueNameTemplate: String): String = {
    val starIndex = queueNameTemplate.indexOf("*")
    if (starIndex != -1) {
      val randomString = "queue-" + queueNum
      queueNum += 1
      return queueNameTemplate.patch(starIndex, randomString, 1)
    }

    queueNameTemplate //return the unique queue name
  }

  def declareQueueInternal(queue: String, autoDelete: Boolean, exclusive: Boolean): String = {
    synchronized { privateName(queue) }
  }

  def declareExchangeInternal(exchange: String, exchangeType: String = "topic"): Unit = {
    synchronized {
      exchanges.get(exchange) match {
        case Some(l) =>
        case None =>
          exchanges = exchanges - exchange + (exchange -> Nil)
      }
    }
  }

  def bindQueueInternal(queue: String, exchange: String, key: String, unbindFirst: Boolean): Unit = {
    if (unbindFirst) {
      unbindQueue(queue, exchange, key)
    }
    synchronized {
      exchanges.get(exchange) match {
        case Some(l: List[_]) =>
          val eb = new ExchangeBinding(key, queue)
          if (!l.contains(eb)) {
            // only add the binding if we dont allready have an exact match
            exchanges = exchanges - exchange + (exchange -> (eb :: l.asInstanceOf[List[ExchangeBinding]]))
          }
        case None =>
          declareExchange(exchange)
          bindQueue(queue, exchange, key, unbindFirst)
      }
    }
  }

  def unbindQueueInternal(queue: String, exchange: String, key: String): Unit = {
    synchronized {
      exchanges.get(exchange) match {
        case Some(l: List[_]) =>
          val eb = new ExchangeBinding(key, queue)
          if (l.contains(eb)) {
            exchanges = exchanges - exchange + (exchange -> l.filterNot(_ == eb))
          }
        case None =>
      }
    }
  }

}