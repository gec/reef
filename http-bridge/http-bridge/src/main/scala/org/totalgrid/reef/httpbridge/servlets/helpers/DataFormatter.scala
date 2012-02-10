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

import com.google.protobuf.Message
import java.io.InputStream
import org.totalgrid.reef.httpbridge.formatters._
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import org.totalgrid.reef.httpbridge.JsonBridgeConstants

/**
 * trait for handling input and output data. In the future these calls can be parametrized
 * with "Content-Type" and "Accept-Type" to switch between different serialization strategies.
 */
trait DataFormatter extends HeaderUtils {

  import JsonBridgeConstants._

  def parseInput[A <: Message.Builder](req: HttpServletRequest, inputStream: InputStream, builder: A): A = {
    getFormatter(req, true).input(inputStream, builder)
    builder
  }

  def printOutput[A <: Message](req: HttpServletRequest, resp: HttpServletResponse, list: List[A]) {
    val formatter = getFormatter(req, false)

    resp.setHeader(CONTENT_TYPE_HEADER, formatter.mimeType)
    formatter.output(resp.getOutputStream, list)
  }

  def printSingleOutput[A <: Message](req: HttpServletRequest, resp: HttpServletResponse, proto: A) {
    val formatter = getFormatter(req, false)

    resp.setHeader(CONTENT_TYPE_HEADER, formatter.mimeType)
    formatter.outputOne(resp.getOutputStream, proto)
  }

  private def getFormatter(req: HttpServletRequest, isInput: Boolean): SimpleFormatter = {
    val headerName = if (isInput) CONTENT_TYPE_HEADER else ACCEPT_HEADER

    // default format is json
    val contentTypeString = Option(req.getHeader(headerName)).getOrElse(JSON_FORMAT)
    val contentTypes = contentTypeString.split(",").map { _.trim() }

    contentTypes.foreach { contentType =>
      contentType match {
        case JSON_FORMAT => return new JsonFormatter
        case PROTOBUF_FORMAT => return new RawProtoFormatter
        case _ =>
      }
    }
    new JsonFormatter
  }

}