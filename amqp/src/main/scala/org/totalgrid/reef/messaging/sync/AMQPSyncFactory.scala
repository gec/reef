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
package org.totalgrid.reef.messaging.sync

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.messaging._

import org.totalgrid.reef.api.{ ServiceHandlerHeaders, ISubscription }
import org.totalgrid.reef.api.ServiceTypes.Event

/**
 * syncronous subscription object, allows canceling
 */
class SyncSubscription[A](channel: BrokerChannel, consumer: MessageConsumer) extends ISubscription[A] {
  private val queue = QueuePatterns.getPrivateUnboundQueue(channel, consumer)
  override def setHeaders(headers: ServiceHandlerHeaders) =
    headers.setSubscribeQueue(queue)
  override def cancel() = channel.close()
}

trait AMQPSyncFactory extends AMQPConnectionReactor with ServiceClientFactory {

  /**
   * creates a correlator channel that can multiplex ServiceRequests to different exchanges and collect the inputs
   * and demultiplexes them back to the right clients.
   */
  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator = {
    val channel = this.getChannel()
    val reqReply = new ProtoSyncRequestReply(channel)
    new ServiceResponseCorrelator(timeoutms, reqReply)
  }

  def prepareSubscription[A <: GeneratedMessage](deserialize: Array[Byte] => A, subIsStreamType: Boolean, callback: Event[A] => Unit): ISubscription[A] = {
    val channel = getChannel()

    val consumer = if (subIsStreamType)
      AMQPMessageConsumers.makeConvertingEventStreamConsumer(deserialize, callback)
    else
      AMQPMessageConsumers.makeEventConsumer(deserialize, callback)

    new SyncSubscription[A](channel, consumer)
  }
}
