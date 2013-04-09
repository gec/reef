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

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.SubscriptionBinding
import org.totalgrid.reef.client.proto.Envelope
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.broker.{ BrokerConnection, BrokerSubscription, BrokerMessage, BrokerMessageConsumer }
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.registration.{ ServiceResponseCallback, Service }

class DefaultServiceBinding[A](conn: BrokerConnection, sub: BrokerSubscription, exe: Executor)
    extends SubscriptionBinding with Logging {

  def cancel() {
    sub.close()
  }

  def getId = sub.getQueue

  def start(service: Service) {
    val consumer = new BrokerMessageConsumer {
      def onMessage(msg: BrokerMessage) {
        exe.execute {
          msg.replyTo match {
            case None => logger.error("Service request without replyTo field")
            case Some(dest) =>
              safely("Error deserializing ServiceRequest") {
                val request = Envelope.ServiceRequest.parseFrom(msg.bytes)
                import collection.JavaConversions._
                val headers = convertHeadersToMap(request.getHeadersList.toList)
                val callback = new ServiceResponseCallback {
                  def onResponse(rsp: Envelope.ServiceResponse) = publish(rsp, dest.exchange, dest.key)
                }
                service.respond(request, headers, callback) //invoke the service, it will publish the result when done
              }
          }
        }
      }
    }
    sub.start(consumer)
  }

  private def convertHeadersToMap(keyValuePairs: List[Envelope.RequestHeader]): java.util.Map[String, java.util.List[String]] = {
    keyValuePairs.map(e => e.getKey -> e.getValue).groupBy(_._1).mapValues(_.map(_._2): java.util.List[String])
  }

  private def publish(rsp: Envelope.ServiceResponse, exchange: String, key: String) {
    conn.publish(exchange, key, rsp.toByteArray, None)
  }

  private def safely(msg: String)(fun: => Unit) {
    try {
      fun
    } catch {
      case ex: Exception => logger.error(msg, ex)
    }
  }
}