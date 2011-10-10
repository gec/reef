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
package org.totalgrid.reef.messaging.synchronous

import org.totalgrid.reef.japi.client.ConnectionListener
import org.totalgrid.reef.japi._
import org.totalgrid.reef.sapi.client._
import org.totalgrid.reef.sapi._
import newclient.{SubscriptionHandler, Bindable, Subscribable}
import service.AsyncService
import org.totalgrid.reef.util.Cancelable

import org.totalgrid.reef.messaging.{ AMQPMessageConsumers, QueuePatterns }
import org.totalgrid.reef.japi.Envelope.ServiceResponse
import org.totalgrid.reef.broker.api._
import net.agileautomata.executor4s._

/** Implements all of the higher-level rest functions */
final class ServiceClient(lookup: ServiceList, conn: BrokerConnection, executor: Executor, timeoutms: Long)
    extends AsyncRestAdapter with Subscribable with Bindable with SubscriptionHandler with DefaultHeaders {

  private val strand = Strand(executor)

  private val listener = new ConnectionListener {
    final override def onConnectionClosed(expected: Boolean) = setState()
    final override def onConnectionOpened() = setState()
  }

  def addListener(listener: ConnectionListener) = conn.addListener(listener)
  def removeListener(listener: ConnectionListener) = conn.removeListener(listener)

  private var state: Option[CurrentState] = None
  private val correlator = new ResponseCorrelator

  conn.addListener(listener)
  this.setState()

  private case class CurrentState(channel: BrokerChannel, queue: String)

  private def setState() = {
    state = if (conn.isConnected) {
      val channel = conn.newChannel()
      val queue = QueuePatterns.getPrivateResponseQueue(channel, "amq.direct", correlator)
      Some(CurrentState(channel, queue))
    } else {
      None
    }
  }

  override protected def asyncRequest[A](
    verb: Envelope.Verb,
    payload: A,
    headers: BasicRequestHeaders)(callback: Response[A] => Unit) = {

    val info = lookup.getServiceInfo(ClassLookup.get(payload))

    def onResponse(result: Option[Envelope.ServiceResponse]) = result match {
      case Some(envelope) => callback(RestOperations.readServiceResponse(info.descriptor, envelope))
      case None => callback(ResponseTimeout)
    }

    state match {
      case Some(CurrentState(channel, queue)) =>
        val uuid = correlator.register(strand, timeoutms.milliseconds)(onResponse)
        val request = RestOperations.buildServiceRequest(verb, payload, info.descriptor, uuid, getHeaders.merge(headers))
        conn.publish(info.descriptor.id, headers.getDestination.key, request.toByteArray, Some(Destination("amq.direct", queue)))
      case None =>
        throw new Exception("Connection is closed, service client cannot perform service call")
    }
  }

  override def prepareSubscription[A](descriptor: TypeDescriptor[A]): Subscription[A] = {
    new SynchronousSubscription[A](conn.newChannel(), executor, descriptor.deserialize)
  }

  override def bindService[A](service: AsyncService[A], destination: Routable = AnyNodeDestination, competing: Boolean = false): Cancelable = {
    def publish(response: ServiceResponse, exchange: String, key: String) = conn.publish(exchange, key, response.toByteArray)
    val channel = conn.newChannel()
    val mc = AMQPMessageConsumers.makeServiceBinding(publish, service.respond)
    val proxy = new MessageConsumer {
      def receive(bytes: Array[Byte], replyTo: Option[Destination]) = executor.execute(mc.receive(bytes, replyTo))
    }

    //declare the subexchange
    val info = lookup.getServiceInfo(service.descriptor.getKlass)
    conn.declareExchange(info.subExchange)

    if (competing) QueuePatterns.getCompetingConsumer(channel, service.descriptor.id, service.descriptor.id + "_server", destination.key, proxy)
    else QueuePatterns.getExclusiveQueue(channel, service.descriptor.id, destination.key, proxy)

    new Cancelable { def cancel() = channel.close() }
  }

  final override def publishEvent[A](typ: Envelope.Event, value: A, key: String): Unit = {
    val info = lookup.getServiceInfo(ClassLookup.get(value))
    val desc = info.subType.asInstanceOf[TypeDescriptor[A]]
    val event = RestOperations.getEvent(typ, value, desc)
    conn.publish(info.subExchange, key, event.toByteArray)
  }

  override def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]): Unit = {
    val info = lookup.getServiceInfo(klass)
    //println("Binding queue " + subQueue +  to " + info.subExchange + " with key " + key)
    conn.bindQueue(subQueue, info.subExchange, key)
  }

}