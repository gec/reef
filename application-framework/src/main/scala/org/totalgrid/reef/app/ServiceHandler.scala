/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.app

import org.totalgrid.reef.util.{ Timer, Logging }

import org.totalgrid.reef.messaging.Connection

import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.sapi.client._

object ServiceHandler {

  // The list of type A you get back in the event of a subscription
  type ResponseHandler[-A] = (List[A]) => Unit

  // A discrete event of type A
  type EventHandler[-A] = (Envelope.Event, A) => Unit
}

class ServiceHandler(executor: Executor) extends Logging {
  import ServiceHandler._

  // helper function
  private def subscribe[A](client: ClientSession, queue: String, searchObj: A, retryMS: Long, subHandler: ResponseHandler[A]): Unit = {

    def handleResponse(rsp: Response[A]) = rsp match {
      case SuccessResponse(_, list) => executor.execute(subHandler(list))
      case FailureResponse(status, msg) =>
        logger.error("Error getting subscription for " + searchObj)
        executor.delay(retryMS)(subscribe(client, queue, searchObj, retryMS, subHandler)) //defined recursively
    }

    val env = BasicRequestHeaders.empty.setSubscribeQueue(queue)

    client.get(searchObj, env).listen(handleResponse)
  }

  /**
   *  Overload of addService that can directly bind to a ServiceContext
   *
   *  @param 	deserialize 	  Message deserialization function
   *  @param	searchObj		    Message to be sent as subscribe request selector
   *  @param  context         Service context to bind the response/events
   *
   */
  def addServiceContext[A](conn: Connection, retryMS: Long, deserialize: Array[Byte] => A, searchObj: A, context: ServiceContext[A]) =
    addService(conn, retryMS, deserialize, searchObj, context.handleResponse _, context.handleEvent _)

  /**
   *  Adds a service subscription and adds callbacks for service responses/events
   *
   *  @param 	deserialize 	    Message deserialization function
   *  @param	searchObj		    Message to be sent as subscribe request selector
   *  @param	responseHandler		Callback for service responses
   *  @param	eventHandler		Callback for service events
   *
   */
  def addService[A](conn: Connection, retryMS: Long, deserialize: Array[Byte] => A, searchObj: A, subHandler: ResponseHandler[A], evtHandler: EventHandler[A]) = {

    // function to call when events occur
    def handleEvent(evt: Event[A]) = executor.execute(evtHandler(evt.event, evt.value))
    def handleNewQueue(queue: String) = conn.getSessionPool().borrow(subscribe(_, queue, searchObj, retryMS, subHandler))

    // ask for a queue, direct all events back to this actor
    conn.defineEventQueueWithNotifier(deserialize, handleEvent)(handleNewQueue)
  }

}
