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

import org.totalgrid.reef.reactor.{ Reactor, ReactActor, Reactable }

import org.totalgrid.reef.api.ServiceTypes._
import org.totalgrid.reef.api.{ Envelope, ISubscription }
import org.totalgrid.reef.api.service.sync.ISyncService

/** Extends the AMQPConnectionReactor with functions for reading and writing google protobuf classes.
 *  
 */
trait AMQPProtoFactory extends AMQPConnectionReactor with ServiceClientFactory {

  /** Configures a publisher that targets a specific exchange
   */
  private def publish(exchange: String): (Array[Byte], String) => Unit = {
    val pub = new AMQPPublisher(exchange :: Nil) with ReactActor
    add(pub)
    addReactor(pub)
    pub.send(_, exchange, _)
  }

  /**
   * starts and binds the reactor to this factory so it gets shutdown at same time as the parent
   * factory
   */
  def addReactor(reactor: Reactor): Unit = {
    bind(reactor.getActor)
    reactor.start()
  }

  /* ---- Functions for subscribing  ---- */

  def subscribe[A](exchange: String, key: String, convert: Array[Byte] => A, accept: A => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPExclusiveConsumer(exchange, key, consumer))
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
    add(new AMQPCompetingConsumer(exchange, queueName, "#", consumer))
  }

  /* ---- publishing functions ---- */

  /**
   * get a publisher that sends messages to a fixed exchange and generates the routing key
   * based off the content of that message
   */
  def publish[A <: GeneratedMessage](exchange: String, keygen: A => String): A => Unit = {
    AMQPConvertingProtoPublisher.wrapSend[A](publish(exchange), keygen)
  }

  /**
   * get a publisher that sends messages to a fixed exchange but allow us to manually set the routing_key
   */
  def send[A <: GeneratedMessage](exchange: String): (A, String) => Unit = {
    AMQPConvertingProtoPublisher.wrapSendWithKey[A](publish(exchange))
  }

  /**
   * get a publisher that allow us to send proto messages to an arbitrary exchange with arbitrary key
   */

  def broadcast[A <: GeneratedMessage](): (A, String, String) => Unit = {
    val pub = new AMQPPublisher with ReactActor
    add(pub)
    addReactor(pub)
    AMQPConvertingProtoPublisher.wrapSendToExchange[A](pub.send(_, _, _))
  }

  /* ---- Service related functions ---- */

  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator = {
    val reqReply = new AMQPServiceRequestReply("amq.direct") with ReactActor
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

  def prepareSubscription[A <: GeneratedMessage](deserialize: Array[Byte] => A, subIsStreamType: Boolean, callback: Event[A] => Unit): ISubscription = {
    // TODO: implement prepareSubscription for async world?
    throw new Exception("Not implemented for asyc factory")
  }

  /* ---- Functions related to implementing services ---- */

  /**
   * bind an AddressableService service handler to the named exchange with default routing key ("request") making it a "well known service"
   */
  def bindService(exchange: String, handlerFun: ISyncService.Respond, competing: Boolean = false, reactor: Option[Reactable] = None): Unit = {
    bindAddressableService(exchange, "request", handlerFun, competing, reactor)
  }

  /**
   * bind a service handler to the bus for a given exchange
   * @param exchange   exchange to bind to
   * @param key        key to bind to the exchange (address of node)
   * @param handlerFun handler for the ServiceRequest, must return ServiceReponse
   * @param competing  false => (everyone gets a copy of the messages) or true => (only one handler gets each message) 
   * @param reactor    if not None messaging handling is dispatched to a user defined reactor using execute
   */
  def bindAddressableService(exchange: String, key: String, handlerFun: ISyncService.Respond, competing: Boolean = false, reactor: Option[Reactable] = None): Unit = {
    val pub = broadcast[Envelope.ServiceResponse]()
    val binding = dispatch(AMQPMessageConsumers.makeServiceBinding(pub, handlerFun), reactor)

    if (competing) add(new AMQPCompetingConsumer(exchange, exchange + "_server", key, binding))
    else add(new AMQPExclusiveConsumer(exchange, key, binding))
  }

  /**
   * if given a reactor dispatch all message processing to that reactor
   */
  private def dispatch(binding: MessageConsumer, reactor: Option[Reactable]): MessageConsumer = {
    reactor match {
      case Some(r) => AMQPMessageConsumers.dispatchToReactor(r, binding)
      case None => binding
    }
  }
}
