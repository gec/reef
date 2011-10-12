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

import net.agileautomata.executor4s._
import org.totalgrid.reef.sapi._

import client.{ Subscription, ResponseTimeout, Response, Failure => FailureResponse }
import newclient.{ Promise, Client, Connection }
import org.totalgrid.reef.broker.newapi._
import org.totalgrid.reef.japi.{ TypeDescriptor, Envelope }
import org.totalgrid.reef.japi.Envelope.Verb
import service.{ ServiceResponseCallback, AsyncService }
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Auth.{ AuthToken, Agent }

final class BasicConnection(lookup: ServiceList, conn: BrokerConnection, executor: Executor, timeoutms: Long) extends Connection with Logging {

  conn.declareExchange("amq.direct")
  private val correlator = new ResponseCorrelator
  private val subscription = conn.listen().start(correlator)
  conn.bindQueue(subscription.getQueue, "amq.direct", subscription.getQueue)

  def login(authToken: String): Client = createClient(authToken, Strand(executor))

  def login(userName: String, password: String): Promise[Client] = {
    val strand = Strand(executor)
    val agent = AuthToken.newBuilder.setAgent(Agent.newBuilder.setName(userName).setPassword(password)).build()
    def convert(response: Response[AuthToken]): Result[Client] = response.one match {
      case Left(ex) => Failure(ex)
      case Right(token) => Success(createClient(token.getToken, strand))
    }
    Promise.from(request(Verb.PUT, agent, BasicRequestHeaders.empty, strand).map(convert))
  }

  private def createClient(authToken: String, strand: Strand) = {
    val client = new BasicClient(this, strand)
    client.modifyHeaders(_.addAuthToken(authToken))
    client
  }

  def request[A](verb: Verb, payload: A, headers: BasicRequestHeaders, executor: Executor): Future[Response[A]] = {

    val future = executor.future[Response[A]]

    def onResponse(descriptor: TypeDescriptor[A])(result: Option[Envelope.ServiceResponse]) = result match {
      case Some(envelope) => future.set(RestHelpers.readServiceResponse(descriptor, envelope))
      case None => future.set(ResponseTimeout)
    }

    def send(info: ServiceInfo[A, _]) = {
      try {
        correlator.register(executor, timeoutms.milliseconds, onResponse(info.descriptor)) { uuid =>
          val request = RestHelpers.buildServiceRequest(verb, payload, info.descriptor, uuid, headers)
          val replyTo = Some(BrokerDestination("amq.direct", subscription.getQueue))
          conn.publish(info.descriptor.id, headers.getDestination.key, request.toByteArray, replyTo)
        }
      } catch {
        case ex: Exception => future.set(FailureResponse(Envelope.Status.BUS_UNAVAILABLE, ex.getMessage))
      }
    }

    ClassLookup(payload).flatMap(lookup.getServiceOption) match {
      case Some(info) => send(info)
      case None => future.set(FailureResponse(Envelope.Status.BAD_REQUEST, "No info on type: " + payload))
    }

    future
  }

  def prepareSubscription[A](exe: Executor, klass: Class[A]): Subscription[A] = {
    val desc = lookup.getServiceInfo(klass).descriptor
    new BasicSubscription[A](conn.listen(), exe, desc.deserialize)
  }

  override def bindService[A](service: AsyncService[A], exe: Executor, destination: Routable, competing: Boolean): Cancelable = {

    def subscribe[A](klass: Class[A], competing: Boolean): BrokerSubscription = {
      val info = lookup.getServiceInfo(klass)
      conn.declareExchange(info.subExchange)
      conn.declareExchange(info.descriptor.id)
      val sub = if (competing) conn.listen(service.descriptor.id + "_server")
      else conn.listen()
      conn.bindQueue(sub.getQueue, info.descriptor.id, destination.key)
      sub
    }

    def publish(rsp: Envelope.ServiceResponse, exchange: String, key: String) = {
      conn.publish(exchange, key, rsp.toByteArray, None)
    }

    val consumer = new BrokerMessageConsumer {
      def onMessage(msg: BrokerMessage) = exe.execute {
        msg.replyTo match {
          case None => logger.error("Service request without replyTo field")
          case Some(dest) =>
            safely("Error deserializing ServiceRequest") {
              val request = Envelope.ServiceRequest.parseFrom(msg.bytes)
              import collection.JavaConversions._
              val headers = BasicRequestHeaders.from(request.getHeadersList.toList)
              val callback = new ServiceResponseCallback {
                def onResponse(rsp: Envelope.ServiceResponse) = publish(rsp, dest.exchange, dest.key)
              }
              service.respond(request, headers, callback) //invoke the service, it will publish the result when done
            }
        }
      }
    }

    val sub = subscribe(service.descriptor.getKlass, competing)
    sub.start(consumer)

    new Cancelable { def cancel() = sub.close() }
  }

  override def publishEvent[A](typ: Envelope.Event, value: A, key: String): Unit = {
    val info = lookup.getServiceInfo(ClassLookup.get(value))
    val desc = info.subType.asInstanceOf[TypeDescriptor[A]]
    val event = RestHelpers.getEvent(typ, value, desc)
    conn.publish(info.subExchange, key, event.toByteArray)
  }

  override def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]): Unit = {
    val info = lookup.getServiceInfo(klass)
    conn.bindQueue(subQueue, info.subExchange, key)
  }

  private def safely(msg: String)(fun: => Unit): Unit = {
    try {
      fun
    } catch {
      case ex: Exception => logger.error(msg, ex)
    }
  }
}