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
package org.totalgrid.reef.httpbridge.servlets

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }

import org.totalgrid.reef.httpbridge.JsonBridgeConstants._
import org.totalgrid.reef.httpbridge.servlets.helpers._

/**
 * "comet lite" polling based subscription manager. Clients make a GET request with the subscription
 * id to retrieve stored events. a DELETE request cancels the subscription and removes the stored events
 */
class SubscriptionServlet(subscriptionManager: SimpleSubscriptionManager) extends ServletBase {

  // we want to keep each individual request relatively small for better latency
  private val MAX_EVENTS_RETURNED = 100

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {

    val id = Option(req.getPathInfo).getOrElse("").stripPrefix("/")

    val holder = subscriptionManager.getValueHolder(id)

    // TODO: fix subscription implementation to include event code
    val eventPayloads = holder.poll(MAX_EVENTS_RETURNED).map { _._2 }
    printOutput(req, resp, eventPayloads)
    resp.setStatus(200)
  }

  override def doDelete(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {

    val id = Option(req.getPathInfo).getOrElse("").stripPrefix("/")

    subscriptionManager.removeValueHolder(id)

    resp.getOutputStream.print("Deleted")
    resp.setHeader(CONTENT_TYPE_HEADER, TEXT_FORMAT)
    resp.setStatus(200)
  }
}
