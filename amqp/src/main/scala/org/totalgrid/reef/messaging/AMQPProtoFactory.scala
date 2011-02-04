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

import org.totalgrid.reef.protoapi.ProtoServiceTypes._
import com.google.protobuf.GeneratedMessage

import javabridge.Subscription
import org.totalgrid.reef.reactor.{ Reactor, ReactActor, Reactable }
import org.totalgrid.reef.proto.Envelope

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

  def subscribe[T](exchange: String, key: String, convert: Array[Byte] => T, accept: T => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPExclusiveConsumer(exchange, key, consumer))
  }

  def subscribe[T](exchange: String, convert: Array[Byte] => T, accept: T => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPPrivateResponseQueueListener(exchange, consumer))
  }

  /**
   * start message flow from an externally prepared (and usually persistent) queue, no binding is done to any exchange
   */
  def listen[T](queueName: String, convert: Array[Byte] => T, accept: T => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPExternallyPreparedQueueListener(queueName, consumer))
  }

  /**
   * setup and start message flow from a shared and non-exclusive queue with fixed name and exchange. competing consumer behavior
   */
  def listen[T](queueName: String, exchange: String, convert: Array[Byte] => T, accept: T => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeStreamConsumer(convert, accept)
    add(new AMQPCompetingConsumer(exchange, queueName, "#", consumer))
  }

  /* ---- publishing functions ---- */

  /**
   * get a publisher that sends messages to a fixed exchange and generates the routing key
   * based off the content of that message
   */
  def publish[T <: GeneratedMessage](exchange: String, keygen: T => String): T => Unit = {
    AMQPConvertingProtoPublisher.wrapSend[T](publish(exchange), keygen)
  }

  /**
   * get a publisher that sends messages to a fixed exchange but allow us to manually set the routing_key
   */
  def send[T <: GeneratedMessage](exchange: String): (T, String) => Unit = {
    AMQPConvertingProtoPublisher.wrapSendWithKey[T](publish(exchange))
  }

  /**
   * get a publisher that allow us to send proto messages to an arbitrary exchange with arbitrary key
   */
  def broadcast[T <: GeneratedMessage](): (T, String, String) => Unit = {
    val pub = new AMQPPublisher with ReactActor
    add(pub)
    addReactor(pub)
    AMQPConvertingProtoPublisher.wrapSendToExchange[T](pub.send(_, _, _))
  }

  /* ---- Service related functions ---- */

  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator = {
    val reqReply = new AMQPServiceRequestReply("amq.direct") with ReactActor
    val correlator = new ServiceResponseCorrelator(timeoutms, reqReply)
    add(reqReply)
    addReactor(reqReply)
    correlator
  }

  def getEventQueue[T](convert: Array[Byte] => T, accept: Event[T] => Unit): ObserverableBrokerObject = {
    val consumer = AMQPMessageConsumers.makeEventConsumer(convert, accept)
    add(new AMQPUnboundPrivateQueueListener(consumer))
  }

  def getEventQueue[T](convert: Array[Byte] => T, accept: Event[T] => Unit, notify: String => Unit): ObservableSubscription = {
    val consumer = AMQPMessageConsumers.makeEventConsumer(convert, accept)
    val sub = new AMQPUnboundPrivateQueueListener(consumer)
    sub.resubscribe(notify)
    add(sub)
  }

  def getStreamQueue[T](convert: Array[Byte] => T, accept: Event[T] => Unit, notify: String => Unit): ObservableSubscription = {
    val consumer = AMQPMessageConsumers.makeConvertingEventStreamConsumer(convert, accept)
    val sub = new AMQPUnboundPrivateQueueListener(consumer)
    sub.resubscribe(notify)
    add(sub)
  }

  def prepareSubscription[T <: GeneratedMessage](deserialize: Array[Byte] => T, subIsStreamType: Boolean, callback: Event[T] => Unit): Subscription = {
    // TODO: implement prepareSubscription for async world?
    throw new Exception("Not implemented for asyc factory")
  }

  /* ---- Functions related to implementing services ---- */

  /**
   * bind an AddressableService service handler to the named exchange with default routing key ("request") making it a "well known service"
   */
  def bindService(exchange: String, handlerFun: ServiceRequestHandler.Respond, competing: Boolean = false, reactor: Option[Reactable] = None): Unit = {
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
  def bindAddressableService(exchange: String, key: String, handlerFun: ServiceRequestHandler.Respond, competing: Boolean = false, reactor: Option[Reactable] = None): Unit = {
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
