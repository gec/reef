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

import org.totalgrid.reef.api.ServiceTypes.Event
import org.totalgrid.reef.api.Subscription

trait AMQPSyncFactory extends AMQPConnectionReactor with ClientSessionFactory {

  /**
   * creates a correlator channel that can multiplex ServiceRequests to different exchanges and collect the inputs
   * and demultiplexes them back to the right clients.
   */
  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator = {
    val channel = this.getChannel()
    val reqReply = new ProtoSyncRequestReply(channel)
    new ServiceResponseCorrelator(timeoutms, reqReply)
  }

  def prepareSubscription[A <: GeneratedMessage](deserialize: Array[Byte] => A, subIsStreamType: Boolean): Subscription[A] = {
    val channel = getChannel()

    val consumer = { a: (Event[A] => Unit) =>
      if (subIsStreamType)
        AMQPMessageConsumers.makeConvertingEventStreamConsumer(deserialize, a)
      else
        AMQPMessageConsumers.makeEventConsumer(deserialize, a)
    }
    new SyncSubscription[A](channel, consumer)
  }
}
