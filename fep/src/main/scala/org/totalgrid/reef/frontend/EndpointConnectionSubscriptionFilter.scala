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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.FEP.CommEndpointConnection
import org.totalgrid.reef.app.{ ServiceContext, ClearableMap }
import org.totalgrid.reef.util.{ Cancelable, Logging }
import org.totalgrid.reef.japi.client.SubscriptionResult

class EndpointConnectionSubscriptionFilter(connections: ClearableMap[CommEndpointConnection], populator: EndpointConnectionPopulatorAction)
    extends ServiceContext[CommEndpointConnection]
    with FepServiceContext
    with Logging {

  var subscription: Option[Cancelable] = None

  def setSubscription(result: SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]) = {
    if (subscription.isDefined) throw new IllegalArgumentException("Subscription already set.")
    subscription = Some(ServiceContext.attachToServiceContext(result, this))
  }

  def cancel() = {
    subscription.foreach(_.cancel())
    connections.clear
    subscription = None
  }

  // all of the objects we receive here are incomplete we need to request
  // the full object tree for them
  def add(c: CommEndpointConnection) = tryWrap("Error adding connProto: " + c) {
    // the coordinator assigns FEPs when available but meas procs may not be online yet
    // re sends with routing information when meas_proc is online
    if (c.hasRouting && c.hasEnabled && c.getEnabled) connections.add(populator.populate(c))
    else connections.remove(c)
  }

  def modify(c: CommEndpointConnection) = tryWrap("Error modifying connProto: " + c) {
    if (c.hasRouting && c.hasEnabled && c.getEnabled) connections.modify(populator.populate(c))
    else connections.remove(c)
  }

  def remove(c: CommEndpointConnection) = tryWrap("Error removing connProto: " + c) {
    connections.remove(c)
  }

  def subscribed(list: List[CommEndpointConnection]) = list.foreach(add(_))

  /**
   * when setting up asynchronous callbacks it is doubly important to catch exceptions
   * near where they are thrown or else they will bubble all the way up into the calling code
   */
  private def tryWrap[A](msg: String)(fun: => A) {
    try {
      fun
    } catch {
      case t: Throwable => logger.error(msg + ": " + t)
    }
  }
}