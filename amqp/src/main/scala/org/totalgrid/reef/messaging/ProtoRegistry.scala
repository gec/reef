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

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.api.scalaclient.ServiceClient
import org.totalgrid.reef.api.{ ServiceList, ServiceTypes, RequestEnv }
import ServiceTypes.Event

/** functions for working with named queues and exchanges rather than 'well known' exchanges queues */
trait ProtoQueueRegistry {

  /** set up a listener that is called whenever a proto is sent to queue */
  def listen[A](deserialize: (Array[Byte]) => A, queueName: String)(accept: A => Unit): Unit

  /** publish a proto to an arbitrary exchange */
  def broadcast[A <: GeneratedMessage](exchangeName: String, keygen: A => String): A => Unit
}

/** Abstracts how services and their associated event queues are retrieved */
trait ProtoServiceRegistry {

  /** Creates a service consumer of type A */
  def getServiceClient(): ServiceClient

  /** Creates an event queue of type A that can be monitored using an ObservableSubscription */
  def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit

  /** Overload that defines the subscription in the function call */
  def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit

}

/** Combines the various registry traits into a single interface */
trait ProtoRegistry extends ProtoServiceRegistry with ProtoQueueRegistry

/** Implements the ProtoRegistry trait to provide a concrete AMQP service implementation */
class AMQPProtoRegistry(factory: AMQPProtoFactory, timeoutms: Long, lookup: ServiceList, defaultEnv: Option[RequestEnv] = None) extends ProtoRegistry {

  def getServiceClient(): ServiceClient = {
    val client = new ProtoClient(factory, lookup, timeoutms)
    defaultEnv.foreach(client.setDefaultHeaders)
    client
  }

  def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit = {
    factory.getEventQueue(deserialize, accept)
  }

  def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit = {
    factory.getEventQueue(deserialize, accept, notify)
  }

  def listen[A](deserialize: (Array[Byte]) => A, queueName: String)(accept: A => Unit): Unit = {
    factory.listen(queueName, deserialize, accept)
  }

  def broadcast[A <: GeneratedMessage](exchangeName: String, keygen: A => String): A => Unit = {
    factory.publish(exchangeName, keygen)
  }

}
