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

import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.app.{ SubscriptionHandlerBase, ServiceContext, ClearableMap }
import net.agileautomata.executor4s.Executor

/**
 * When we have subscribed to handle a set of endpoints we need to make sure that we only add enabled and routed
 * endpoints, we remove all other endpoints.
 *
 * Keep in mind that most "live system" updates are going to be modifies of the enabled bit
 */
class EndpointConnectionSubscriptionFilter(connections: ClearableMap[EndpointConnection], populator: EndpointConnectionPopulatorAction, exe: Executor)
    extends ServiceContext[EndpointConnection]
    with SubscriptionHandlerBase[EndpointConnection]
    with Logging {

  // all of the objects we receive here are incomplete we need to request
  // the full object tree for them
  def add(c: EndpointConnection) = tryWrap("Error adding connProto: " + c) {
    logEndpointMessage(c, "ADD")
    // the coordinator assigns FEPs when available but meas procs may not be online yet
    // re sends with routing information when meas_proc is online
    if (shouldStart(c)) connections.add(populator.populate(c))
    else connections.remove(c)
  }

  def modify(c: EndpointConnection) = tryWrap("Error modifying connProto: " + c) {
    logEndpointMessage(c, "MODIFY")
    if (shouldStart(c)) connections.modify(populator.populate(c))
    else connections.remove(c)
  }

  def remove(c: EndpointConnection) = tryWrap("Error removing connProto: " + c) {
    logEndpointMessage(c, "REMOVE")
    connections.remove(c)
  }

  def subscribed(list: List[EndpointConnection]) = exe.execute { list.foreach(add(_)) }

  def clear() = exe.attempt { connections.clear() }.await

  /**
   * we will get messages regardless of whether the endpoint is usable, we need to check that it is
   * still enabled and that there is a routing key (meas proc is ready for us) before adding it.
   * otherwise we remove it
   */
  private def shouldStart(c: EndpointConnection) = c.hasRouting && c.hasEnabled && c.getEnabled && (!c.hasActive || c.getActive)
  // TODO: in 0.5.x we can remove the hasActive part because we assume all servers are 0.4.8+

  // temporary logging to help track down intermittent error
  private def logEndpointMessage(c: EndpointConnection, verb: String) {
    logger.info(verb + " : " + c.getEndpoint.getName + " enabled: " + c.getEnabled +
      " state: " + c.getState + " active: " + (!c.hasActive || c.getActive) +
      " routing: " + c.getRouting.getServiceRoutingKey +
      " adapter: " + c.getFrontEnd.getAppConfig.getInstanceName)
  }

  /**
   * when setting up asynchronous callbacks it is doubly important to catch exception
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