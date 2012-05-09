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

import java.io.PrintStream
import org.totalgrid.reef.client.service.proto.Model.ReefID
import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection, Endpoint }

import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor }

import java.util.concurrent.{ TimeUnit, LinkedBlockingDeque }
import com.weiglewilczek.slf4s.Logging

object EndpointStopper extends Logging {
  /**
   * synchronous function that blocks until all passed in endpoints are stopped
   * TODO: use endpoint stopper before reef:unload
   */
  def stopEndpoints(local: LoaderServices, targetEndpoints: List[Endpoint], stream: Option[PrintStream], forceStop: Boolean, timeout: Long = 20000) {

    // then subscribe to all of the connections
    val subResult = local.subscribeToEndpointConnections().await

    // filter the subscription down to just the endpoints we care about
    val targetUuids = targetEndpoints.map { _.getUuid }
    val filteredEndpoints = subResult.getResult.toList.filter(p => targetUuids.contains(p.getEndpoint.getUuid))

    def endpointNames(endpoints: Map[ReefID, EndpointConnection]) = endpoints.values.map { _.getEndpoint.getName }.mkString(", ")
    val endpointUuids = filteredEndpoints.map { e => e.getId -> e }.toMap

    stream.foreach { _.println("Disabling endpoints: " + endpointNames(endpointUuids)) }

    // first we disable all of the endpoints
    targetEndpoints.foreach { e => local.disableEndpointConnection(e.getUuid).await }

    try {
      // we filter out all of the endpoints that are not COMMS_UP
      val stillRunning = filteredEndpoints.foldLeft(endpointUuids)(filterEndpoints)

      // if there are any still running we need to wait for them to go down
      if (endpointUuids.size > 0) {

        // start the subscription, shunting all incoming values to a queue
        val queue = new LinkedBlockingDeque[EndpointConnection]()
        subResult.getSubscription.start(new SubscriptionEventAcceptor[EndpointConnection] {
          def onEvent(event: SubscriptionEvent[EndpointConnection]) {
            val conn = event.getValue
            logger.info("EndpointChange " + event.getEventType + " " + conn.getEndpoint.getName + " e: " + conn.getEnabled + " s: " + conn.getState)
            queue.push(conn)
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
        def waitForEmptyList(endpointUuids: Map[ReefID, EndpointConnection], timeout: Long): Map[ReefID, EndpointConnection] = {
          if (!endpointUuids.isEmpty) {
            stream.foreach { _.println("Waiting for " + endpointNames(endpointUuids) + " to stop...") }
            val nextEvent = queue.poll(timeout, TimeUnit.MILLISECONDS)
            if (nextEvent == null) endpointUuids
            else waitForEmptyList(filterEndpoints(endpointUuids, nextEvent), timeout)
          } else {
            endpointUuids
          }
        }

        // wait until all endpoints are not COMMS_UP
        val stillOnline = waitForEmptyList(stillRunning, timeout)

        if (!stillOnline.isEmpty) {
          val msg = "Couldn't stop all endpoints: " + endpointNames(stillOnline)
          if (!forceStop) throw new Exception(msg)
          else {
            println(msg)
            println("Forcing endpoints offline.")
            stillOnline.foreach {
              case (id, name) =>
                local.alterEndpointConnectionState(id, EndpointConnection.State.ERROR).await()
            }
          }
        }

        stream.foreach { _.println("Endpoints stopped.") }
      }
    } finally {
      subResult.getSubscription.cancel()
    }
  }

  private def filterEndpoints(stillRunningEndpoints: Map[ReefID, EndpointConnection], connection: EndpointConnection) = {
    if (connection.getState != EndpointConnection.State.COMMS_UP && !connection.hasFrontEnd) {
      stillRunningEndpoints - connection.getId
    } else {
      stillRunningEndpoints
    }
  }

}