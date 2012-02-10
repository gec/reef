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

import javax.servlet.http.HttpServletResponse
import org.totalgrid.reef.client.exception.ReefServiceException
import com.weiglewilczek.slf4s.Logging

import org.totalgrid.reef.httpbridge.JsonBridgeConstants._

/**
 * handles any exceptions thrown in the servlet, returns appropriate HTTP error codes
 */
trait ServletErrorHandler extends Logging {
  def handleErrors[A](resp: HttpServletResponse)(fun: => Unit) {
    try {
      // set the origin header on all requests (even error cases)
      resp.setHeader(ORIGIN_HEADER, "*")
      // IE likes to aggressively cache ajax requests
      resp.setHeader("Cache-Control", "max-age=0,no-cache,no-store")
      fun
    } catch {
      case rse: ReefServiceException =>
        logger.warn("ReefServiceException: " + rse.getMessage, rse)
        val httpCode = rse.getStatus.getNumber match {
          case 401 => 401
          case x if (x >= 400 && x < 500) => 400
          case x if (x >= 500 && x < 600) => 500
          case _ => rse.getStatus.getNumber
        }
        resp.sendError(httpCode, rse.getMessage)
      case ex: Exception =>
        logger.error("Unexpected error: " + ex.getMessage, ex)
        resp.sendError(500, "Unexpected error: " + ex.getMessage)
    }
  }
}