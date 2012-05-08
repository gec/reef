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
package org.totalgrid.reef.client.impl

import org.totalgrid.reef.client.proto.Envelope
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.operations.Response
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.operations.impl.FuturePromise
import org.totalgrid.reef.client.operations.scl.ScalaResponse
import org.totalgrid.reef.client.types.{ ServiceTypeInformation, TypeDescriptor }
import org.totalgrid.reef.client.operations.scl.ScalaRequestHeaders._
import org.totalgrid.reef.broker.{ BrokerDestination, BrokerConnection }
import org.totalgrid.reef.client.{ AnyNodeDestination, Promise, RequestHeaders }

trait RequestManager {

  def request[A](verb: Envelope.Verb, payload: A, headers: RequestHeaders, info: ServiceTypeInformation[A, _], requestExecutor: Executor): Promise[Response[A]]

  def close()
}

object RequestManager {

  private val exchange = "amq.direct"

  def apply(broker: BrokerConnection, executor: Executor, defaultTimeoutMs: Long) = new RequestManagerImpl(broker, executor, defaultTimeoutMs)

  class RequestManagerImpl(broker: BrokerConnection, executor: Executor, defaultTimeoutMs: Long) extends RequestManager with Logging {

    broker.declareExchange(exchange)
    private val correlator = ResponseCorrelator(executor)
    private val subscription = broker.listen().start(correlator)
    broker.bindQueue(subscription.getQueue, exchange, subscription.getQueue)
    //broker.addListener()

    def close() {
      // TODO: is this right?
      correlator.close()
      subscription.close()
    }

    import ResponseCorrelator.Failure

    def request[A](verb: Verb, payload: A, headers: RequestHeaders, info: ServiceTypeInformation[A, _], requestExecutor: Executor): Promise[Response[A]] = {

      val promise = FuturePromise.open[Response[A]](requestExecutor)

      def onResponse(descriptor: TypeDescriptor[A])(result: Either[Failure, Envelope.ServiceResponse]) {
        result match {
          case Left(fail) => promise.setSuccess(ScalaResponse.failure(fail.status, fail.msg))
          case Right(envelope) => promise.setSuccess(RestHelpers.readServiceResponse(descriptor, envelope))
        }
      }

      def send(info: ServiceTypeInformation[A, _]) {
        val timeout = headers.timeout.getOrElse(defaultTimeoutMs)
        val descriptor = info.getDescriptor

        // timeout callback will come in on a random executor thread and be marshalled correctly by future
        val uuid = correlator.register(timeout, onResponse(descriptor))
        try {
          val request = RestHelpers.buildServiceRequest(verb, payload, descriptor, uuid, headers)
          val replyTo = Some(BrokerDestination("amq.direct", subscription.getQueue))
          val destination = headers.destination.getOrElse(new AnyNodeDestination)
          broker.publish(descriptor.id, destination.getKey, request.toByteArray, replyTo)
        } catch {
          case ex: Exception =>
            correlator.fail(uuid, Failure(Envelope.Status.BUS_UNAVAILABLE, ex.getMessage))
        }
      }

      send(info)

      promise
    }
  }

}

