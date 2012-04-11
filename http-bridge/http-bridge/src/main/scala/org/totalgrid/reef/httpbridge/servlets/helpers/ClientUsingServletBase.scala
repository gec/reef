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
package org.totalgrid.reef.httpbridge.servlets.helpers

import org.totalgrid.reef.httpbridge.ManagedConnection
import org.totalgrid.reef.client.exception.{ UnauthorizedException, BadRequestException }
import javax.servlet.http.HttpServletRequest
import org.totalgrid.reef.httpbridge.JsonBridgeConstants._
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.util.Unappliers

/**
 * base class for servlets that use clients to encapsulate common auth and connection specific functionality
 */
class ClientUsingServletBase(connection: ManagedConnection) extends ServletBase {

  def getAuthToken(req: HttpServletRequest) = {

    // check headers, then url parameters, lastly check to see if a default user is configured
    val combinedOption = Option(req.getHeader(AUTH_HEADER))
      .orElse(Option(req.getParameter(AUTH_HEADER)))
      .orElse(connection.getSharedBridgeAuthToken())
    combinedOption.getOrElse(throw new UnauthorizedException("Must include " + AUTH_HEADER + " in headers or URL parameters. No default user available on bridge."))
  }

  def getReefRequestHeaders(req: HttpServletRequest): BasicRequestHeaders = {
    var requestHeaders = BasicRequestHeaders.empty

    // check to see if they want to change the underlying client headers
    optionalLong(req, TIMEOUT_HEADER).foreach { l => requestHeaders = requestHeaders.setTimeout(l) }
    optionalLong(req, RESULT_LIMIT_HEADER).foreach { l => requestHeaders = requestHeaders.setResultLimit(l.toInt) }

    requestHeaders
  }

  private def optionalLong(req: HttpServletRequest, headerName: String): Option[Long] = {
    import Unappliers.Long
    Option(req.getHeader(headerName)).map { headerString =>
      Long.unapply(headerString) match {
        case Some(value) =>
          if (value <= 0) throw new BadRequestException("Header " + headerName + ": " + headerString + " needs to be larger than 0")
          value
        case None => throw new BadRequestException("Header " + headerName + ": " + headerString + " is not convertible to a number")
      }
    }
  }
}