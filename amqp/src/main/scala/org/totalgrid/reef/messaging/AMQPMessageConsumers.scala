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

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.reactor.Reactable

import org.totalgrid.reef.api._
import org.totalgrid.reef.api.scalaclient._
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.api.service.{ IServiceAsync, IServiceResponseCallback }

import org.totalgrid.reef.broker.{ MessageConsumer, Destination }

object AMQPMessageConsumers extends Logging {

  def makeStreamConsumer[A](deserialize: Array[Byte] => A, accept: A => Unit): MessageConsumer = {
    new MessageConsumer {
      def receive(data: Array[Byte], reply: Option[Destination]) = {
        safeExecute {
          accept(deserialize(data))
        }
      }
    }
  }

  def makeEventConsumer[A](convert: Array[Byte] => A, accept: Event[A] => Unit): MessageConsumer = {
    new MessageConsumer {
      def receive(data: Array[Byte], reply: Option[Destination]) = {
        safeExecute {
          val evt = Envelope.ServiceNotification.parseFrom(data)
          accept(new Event(evt.getEvent, convert(evt.getPayload.toByteArray)))
        }
      }
    }
  }

  def makeConvertingEventStreamConsumer[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): MessageConsumer = {
    new MessageConsumer {
      def receive(data: Array[Byte], reply: Option[Destination]) = {
        safeExecute {
          accept(new Event(Envelope.Event.MODIFIED, deserialize(data)))
        }
      }
    }
  }

  /**
   * Provides a receive function that binds a service to a publisher
   *
   * @param publish publishes response to return exchange/request
   * @param service Service handler used to get responses
   */
  def makeServiceBinding(publish: (Envelope.ServiceResponse, String, String) => Unit, service: IServiceAsync.ServiceFunction): MessageConsumer = {

    new MessageConsumer {
      def receive(data: Array[Byte], replyTo: Option[Destination]): Unit = safeExecute {

        replyTo match {
          case None => error("Service request without replyTo field")
          case Some(dest) =>

            val request = Envelope.ServiceRequest.parseFrom(data)
            val rspExchange = dest.exchange
            val rspKey = dest.key

            import _root_.scala.collection.JavaConversions._
            import ServiceHandlerHeaders._

            val env = new RequestEnv
            // convert the headers back into the RequestEnv object
            request.getHeadersList.toList.foreach(h => env.addHeader(h.getKey, h.getValue))

            val callback = new IServiceResponseCallback {
              def onResponse(rsp: Envelope.ServiceResponse) = publish(rsp, rspExchange, rspKey)
            }

            service(request, env, callback) //invoke the service, it will publish the result when done
        }
      }
    }

  }

  /**
   * push the receive to another thread using a reactor
   */
  def dispatchToReactor[A](reactor: Reactable, binding: MessageConsumer): MessageConsumer = {
    new MessageConsumer {
      def receive(data: Array[Byte], reply: Option[Destination]) = {
        reactor.execute {
          safeExecute {
            binding.receive(data, reply)
          }
        }
      }
    }
  }

  /**
   * run the passed in function in a try/catch block with common error handling
   */
  private def safeExecute[A](fun: => A) {
    try {
      fun
    } catch {
      case e: Exception =>
        error(e)
        error(e.getStackTraceString)
    }
  }
}

