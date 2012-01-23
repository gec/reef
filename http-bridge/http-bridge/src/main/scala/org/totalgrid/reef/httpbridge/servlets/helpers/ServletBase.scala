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

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest, HttpServlet }
import org.totalgrid.reef.httpbridge.JsonBridgeConstants._

/**
 * base class for all of the servlets. Includes all of the helper traits
 */
abstract class ServletBase extends HttpServlet with DataFormatter with ServletErrorHandler {
  /**
   * browsers that implement CORS will send an OPTION request to the server first:
   * https://developer.mozilla.org/En/HTTP_access_control
   * http://enable-cors.org/
   */
  override def doOptions(req: HttpServletRequest, resp: HttpServletResponse) {
    resp.setHeader(ORIGIN_HEADER, "*")
    resp.setHeader("Access-Control-Allow-Headers", CUSTOM_HEADERS.mkString(", "))
    resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
    resp.setStatus(200)
  }
}