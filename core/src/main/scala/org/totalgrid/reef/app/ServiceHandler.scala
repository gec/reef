/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.app

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.util.{ Timer, Logging }
import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.protoapi.client.ServiceClient

import org.totalgrid.reef.messaging.ProtoRegistry

import org.totalgrid.reef.protoapi.{ RequestEnv, ServiceHandlerHeaders, ProtoServiceTypes }
import ProtoServiceTypes.{ Failure, MultiResponse, Event }

import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders //implicit

object ServiceHandler {

  // The list of type A you get back in the event of a subscription
  type ResponseHandler[-A] = (List[A]) => Unit

  // A discrete event of type A
  type EventHandler[-A] = (Envelope.Event, A) => Unit
}

trait ServiceHandler extends Logging {
  import ServiceHandler._
  // assume that the host class has this function
  def execute(fun: => Unit)

  // helper function
  private def subscribe[A <: GeneratedMessage](client: ServiceClient, queue: String, searchObj: A, retryMS: Long, subHandler: ResponseHandler[A]): Unit = {
    val env = new RequestEnv
    env.setSubscribeQueue(queue)
    client.async_get(searchObj, env) {
      _ match {
        case x: Failure =>
          error("Error getting subscription for " + x.toString)
          Timer.delay(retryMS) {
            subscribe(client, queue, searchObj, retryMS, subHandler) //defined recursively
          }
        case MultiResponse(list) =>
          execute(subHandler(list))
      }
    }
  }

  /**
   *  Overload of addService that can directly bind to a ServiceContext
   *
   *  @param 	deserialize 	  Message deserialization function
   *  @param	searchObj		    Message to be sent as subscribe request selector
   *  @param  context         Service context to bind the response/events
   *
   */
  def addServiceContext[A <: GeneratedMessage](registry: ProtoRegistry, retryMS: Long, deserialize: Array[Byte] => A, searchObj: A, context: ServiceContext[A]) =
    addService(registry, retryMS, deserialize, searchObj, context.handleResponse _, context.handleEvent _)

  /**
   *  Adds a service subscription and adds callbacks for service responses/events
   *
   *  @param 	deserialize 	    Message deserialization function
   *  @param	searchObj		    Message to be sent as subscribe request selector
   *  @param	responseHandler		Callback for service responses
   *  @param	eventHandler		Callback for service events
   *
   */
  def addService[A <: GeneratedMessage](registry: ProtoRegistry, retryMS: Long, deserialize: Array[Byte] => A, searchObj: A, subHandler: ResponseHandler[A], evtHandler: EventHandler[A]) = {

    // service client which does subscribe calls
    val client = registry.getServiceClient(deserialize)

    // function to call when events occur
    val evtFun = { evt: Event[A] =>
      execute(evtHandler(evt.event, evt.result))
    }

    // function to call when a new queue arrives
    val queueFun: String => Unit = { queue: String =>
      subscribe(client, queue, searchObj, retryMS, subHandler)
    }

    // ask for a queue, direct all events back to this actor
    registry.defineEventQueueWithNotifier(deserialize, evtFun)(queueFun)

    client
  }

}
