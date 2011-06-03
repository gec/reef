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

import org.totalgrid.reef.executor.{ ActorExecutor, ReactActorExecutor, Executor }

import org.totalgrid.reef.sapi._
import client._
import service.AsyncService

import org.totalgrid.reef.japi.Envelope

import java.io.Closeable
import org.totalgrid.reef.broker.{ ChannelObserver, CloseableChannel, MessageConsumer }

/**
 * Extends the AMQPConnectionReactor with functions for reading and writing google protobuf classes.
 *
 */
trait AMQPProtoFactory extends AMQPConnectionReactor with ClientSessionFactory {

  /**
   * Configures a publisher that targets a specific exchange
   */
  private def publish(exchange: String): (Array[Byte], String) => Unit = {
    val pub = new AMQPPublisher(exchange :: Nil) with ReactActorExecutor
    add(pub)
    addReactor(pub)
    pub.send(_, exchange, _)
  }

  /**
   * starts and binds the reactor to this factory so it gets shutdown at same time as the parent
   * factory
   */
  def addReactor(reactor: ActorExecutor): Unit = {
    bind(reactor.getActor)
    reactor.start()
  }

  /* ---- Functions for subscribing  ---- */

  def subscribe[A](exchange: String, routingKey: RoutingKey, convert: Array[Byte] => A, accept: A => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPExclusiveConsumer(exchange, routingKey, consumer))
  }

  def subscribe[A](exchange: String, convert: Array[Byte] => A, accept: A => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPPrivateResponseQueueListener(exchange, consumer))
  }

  /**
   * start message flow from an externally prepared (and usually persistent) queue, no binding is done to any exchange
   */
  def listen[A](queueName: String, convert: Array[Byte] => A, accept: A => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPExternallyPreparedQueueListener(queueName, consumer))
  }

  /**
   * setup and start message flow from a shared and non-exclusive queue with fixed name and exchange. competing consumer behavior
   */
  def listen[A](queueName: String, exchange: String, convert: Array[Byte] => A, accept: A => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPCompetingConsumer(exchange, queueName, AllMessages, consumer))
  }

  /* ---- publishing functions ---- */

  /**
   * get a publisher that sends messages to a fixed exchange and generates the routing key
   * based off the content of that message
   */
  def publish[A](exchange: String, keygen: A => String, serialize: A => Array[Byte]): A => Unit = {
    AMQPConvertingProtoPublisher.wrapSend[A](publish(exchange), keygen, serialize)
  }

  /**
   * get a publisher that sends messages to a fixed exchange but allow us to manually set the routing_key
   */
  def send[A](exchange: String, serialize: A => Array[Byte]): (A, String) => Unit = {
    AMQPConvertingProtoPublisher.wrapSendWithKey[A](publish(exchange), serialize)
  }

  /**
   * get a publisher that allow us to send proto messages to an arbitrary exchange with arbitrary key
   */

  def broadcast[A](serialize: A => Array[Byte]): (A, String, String) => Unit = {
    val pub = new AMQPPublisher with ReactActorExecutor
    add(pub)
    addReactor(pub)
    AMQPConvertingProtoPublisher.wrapSendToExchange[A](pub.send(_, _, _), serialize)
  }

  /* ---- Service related functions ---- */

  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator = {
    val reqReply = new AMQPServiceRequestReply("amq.direct") with ReactActorExecutor
    val correlator = new ServiceResponseCorrelator(timeoutms, reqReply)
    add(reqReply)
    addReactor(reqReply)
    correlator
  }

  def getEventQueue[A](convert: Array[Byte] => A, accept: Event[A] => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeEventConsumer(convert, accept)
    add(new AMQPUnboundPrivateQueueListener(consumer))
  }

  def getEventQueue[A](convert: Array[Byte] => A, accept: Event[A] => Unit, notify: String => Unit): ObservableSubscription = {
    val consumer = AMQPMessageConsumers.makeEventConsumer(convert, accept)
    val sub = new AMQPUnboundPrivateQueueListener(consumer)
    sub.resubscribe(notify)
    add(sub)
  }

  def getStreamQueue[A](convert: Array[Byte] => A, accept: Event[A] => Unit, notify: String => Unit): ObservableSubscription = {
    val consumer = AMQPMessageConsumers.makeConvertingEventStreamConsumer(convert, accept)
    val sub = new AMQPUnboundPrivateQueueListener(consumer)
    sub.resubscribe(notify)
    add(sub)
  }

  final override def prepareSubscription[A](deserialize: Array[Byte] => A, subIsStreamType: Boolean): Subscription[A] = {
    // TODO: implement prepareSubscription for async world?
    throw new Exception("Not implemented for asyc factory")
  }

  /* ---- Functions related to implementing services ---- */

  /**
   * bind a service handler to the bus for a given exchange
   * @param exchange   exchange to bind to
   * @param service handler for the ServiceRequest, must return ServiceReponse
   * @param destination Optionally overrides the default destination of AnyNodeDestinationDestination
   * @param competing  false => (everyone gets a copy of the messages) or true => (only one handler gets each message)
   * @param reactor    if not None messaging handling is dispatched to a user defined reactor using execute
   */
  def bindService(exchange: String, service: AsyncService.ServiceFunction, destination: Destination = AnyNodeDestination, competing: Boolean = false, reactor: Option[Executor] = None): CloseableChannel = {
    val pub = broadcast[Envelope.ServiceResponse]((x: Envelope.ServiceResponse) => x.toByteArray)
    val binding = dispatch(AMQPMessageConsumers.makeServiceBinding(pub, service), reactor)

    val s = if (competing) add(new AMQPCompetingConsumer(exchange, exchange + "_server", destination, binding))
    else add(new AMQPExclusiveConsumer(exchange, destination, binding))
    closeable(s)
  }

  private def closeable(s: ChannelObserver with CloseableChannel) = {
    new CloseableChannel {
      def close() {
        remove(s)
        s.close
      }
    }
  }

  /**
   * if given a reactor dispatch all message processing to that reactor
   */
  private def dispatch(binding: MessageConsumer, reactor: Option[Executor]): MessageConsumer = {
    reactor match {
      case Some(r) => AMQPMessageConsumers.dispatchToReactor(r, binding)
      case None => binding
    }
  }
}
