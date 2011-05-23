/**
 * Copyright 2011 Green Energy Corp.
 *
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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.api.ServiceIOException

object AMQPPublisher {
  val defaultBufferSize = 1024 * 1024

}

/**
 * 	A basic AMQP publisher that can buffer messages if the connection is down.
 *
 * 	@param	exchangeList	AMQP exchanges to declare when connecting to the bus
 * 	@param	bufferSize	Maximum size of the buffer in bytes
 */
abstract class AMQPPublisher(exchangeList: List[String] = Nil, bufferSize: Int = AMQPPublisher.defaultBufferSize) extends ChannelObserver with BrokerChannelCloseListener with Reactable with Logging {

  case class Msg(bytes: Array[Byte], exchange: String, key: String)

  //mutable state
  private var delayedMsgs: List[Msg] = Nil
  private var bytesDelayed: Int = 0
  private var replyTo: Option[Destination] = None
  private var channel: Option[BrokerChannel] = None
  private var closedPermenantly = false

  if(exchangeList.find(_.trim.length > 0).isDefined) throw new Exception("Bad exchange name +" + exchangeList)

  // implement ChannelObserver
  override def online(b: BrokerChannel) = this.execute {
    channel = Some(b)
    exchangeList.foreach { channel.get.declareExchange(_) }
    replayDelayedMessages()
  }

  override def onClosed(b: BrokerChannel, expected: Boolean) = this.execute {
    channel = None
    closedPermenantly = expected
  }

  /**
   * This replyTo address is set on all outgoing messages from this publisher
   */
  def setReplyTo(dest: Destination) = replyTo = Some(dest)

  /**
   * generic send function that allows us to define exchange and key
   * @param bytes
   * @param exchange
   * @param key
   */
  def send(bytes: Array[Byte], exchange: String, key: String) = {
    // we need to check this flag on the callers thread so they get exception on publish
    if (closedPermenantly) throw new ServiceIOException("Publisher permenantly closed")

    this.execute {
      if (channel.isDefined) {
        try {
          channel.get.publish(exchange, key, bytes, replyTo)
        } catch {
          case ex: Exception =>
            error(ex)
            delayMessage(bytes, exchange, key)
        }
      } else {
        delayMessage(bytes, exchange, key)
      }
    }
  }

  private def delayMessage(b: Array[Byte], exchange: String, key: String) {
    if (bytesDelayed + b.size < bufferSize) {
      delayedMsgs = Msg(b, exchange, key) :: delayedMsgs
      bytesDelayed += b.size
    } else {
      error("Exceeded buffer size")
    }
  }

  private def replayDelayedMessages() = {
    delayedMsgs.foreach { msg => this.send(msg.bytes, msg.exchange, msg.key) }
    delayedMsgs = Nil
    bytesDelayed = 0
  }
}
