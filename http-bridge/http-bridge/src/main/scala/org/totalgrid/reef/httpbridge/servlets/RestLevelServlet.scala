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

import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.httpbridge.servlets.helpers.ClientUsingServletBase
import org.totalgrid.reef.httpbridge.{ JsonBridgeConstants, BuilderLocator, ManagedConnection }
import scala.collection.JavaConversions._

/**
 * Provides low-level Rest operations like the ClientOperations api in java.
 */
class RestLevelServlet(connection: ManagedConnection, builderLocator: BuilderLocator, requester: ServiceRequestDelegate = ServiceRequestDelegate) extends ClientUsingServletBase(connection) {

  import JsonBridgeConstants._

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {
    val authToken = getAuthToken(req)
    val verbString = Option(req.getHeader(VERB_HEADER)).getOrElse(throw new BadRequestException("Must include " + VERB_HEADER + " in headers"))
    val verb = try {
      Verb.valueOf(verbString)
    } catch {
      case ex: IllegalArgumentException =>
        throw new BadRequestException(VERB_HEADER + " must be one of: " + Verb.values().toList + " not: " + verbString)
    }
    // get the headers early
    val headers = getReefRequestHeaders(req)
    // get a builder object for parsing (probably a better way to do this)
    val builder = builderLocator.getBuilder(req)

    // parse the input data into the message object
    val requestProto = parseInput(req, req.getInputStream, builder).build()

    // make the request to reef, will fail if we don't have valid connection to reef
    val client = connection.getAuthenticatedClient(authToken)

    val promise = requester.makeRequest(client, verb, requestProto, headers)

    // once we have an async servlet we could use .listen on the future, for now we can use .await
    val response = promise.await()

    // handle response. (note: status code map 1-to-1 with HTTP codes)
    if (response.isSuccess) {
      printOutput(req, resp, response.getList.toList)
      resp.setStatus(response.getStatus.getNumber)
    } else {
      // on error pipe the message out to the client
      resp.sendError(response.getStatus.getNumber, response.getErrorMessage)
    }
  }

}