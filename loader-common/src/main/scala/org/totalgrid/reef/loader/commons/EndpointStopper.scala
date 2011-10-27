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
package org.totalgrid.reef.loader.commons

import scala.collection.JavaConversions._

import java.io.PrintStream
import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection, CommEndpointConfig }

import org.totalgrid.reef.clientapi.{ SubscriptionEvent, SubscriptionEventAcceptor }

import java.util.concurrent.{ TimeUnit, LinkedBlockingDeque }

object EndpointStopper {
  /**
   * synchronous function that blocks until all passed in endpoints are stopped
   * TODO: use endpoint stopper before reef:unload
   */
  def stopEndpoints(local: LoaderServices, endpoints: List[CommEndpointConfig], stream: Option[PrintStream]) {

    val endpointUuids = endpoints.map { _.getUuid }

    stream.foreach { _.println("Disabling endpoints: " + endpoints.map { _.getName }.mkString(", ")) }

    // first we disable all of the endpoints
    endpointUuids.foreach { local.disableEndpointConnection(_).await }

    // then subscribe to all of the connections
    val subResult = local.subscribeToAllEndpointConnections().await
    try {
      // we filter out all of the endpoints that are not COMMS_UP
      val stillRunning = subResult.getResult.toList.foldLeft(endpointUuids)(filterEndpoints)

      // if there are any still running we need to wait for them to go down
      if (endpointUuids.size > 0) {

        // start the subscription, shunting all incoming values to a queue
        val queue = new LinkedBlockingDeque[CommEndpointConnection]()
        subResult.getSubscription.start(new SubscriptionEventAcceptor[CommEndpointConnection] {
          def onEvent(event: SubscriptionEvent[CommEndpointConnection]) {
            queue.push(event.getValue)
          }
        })

        /**
         * if empty return, otherwise try to pop a subscription update off the queue and filter
         * that from the list. Eventually the list will be empty or we will wait more than 5 seconds
         * without there being an update and we'll consider it to have failed.
         * NOTE: if there are constant endpoint online/offline messages in system this function will
         * never return
         */
        @annotation.tailrec
        def waitForEmptyList(endpointUuids: List[ReefUUID], timeout: Int) {
          if (!endpointUuids.isEmpty) {
            stream.foreach { _.println("Waiting for " + endpointUuids.size + " endpoints to stop...") }
            val nextEvent = queue.poll(timeout, TimeUnit.MILLISECONDS)
            if (nextEvent == null) throw new Exception("Couldn't stop all endpoints.")
            waitForEmptyList(filterEndpoints(endpointUuids, nextEvent), timeout)
          }
        }

        // wait until all endpoints are COMMS_DOWN
        waitForEmptyList(stillRunning, 5000)

        stream.foreach { _.println("Endpoints stopped.") }
      }
    } finally {
      subResult.getSubscription.cancel()
    }
  }

  private def filterEndpoints(stillRunningEndpoints: List[ReefUUID], connection: CommEndpointConnection) = {
    if (connection.getState != CommEndpointConnection.State.COMMS_UP) {
      stillRunningEndpoints.filterNot { _ == connection.getEndpoint.getUuid }
    } else {
      stillRunningEndpoints
    }
  }

}