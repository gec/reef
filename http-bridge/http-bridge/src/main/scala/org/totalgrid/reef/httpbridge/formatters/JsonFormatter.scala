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
package org.totalgrid.reef.httpbridge.formatters

import com.googlecode.protobuf.format.JsonJacksonFormat
import com.google.protobuf.Message
import java.io.InputStream
import javax.servlet.{ ServletOutputStream, ServletException }
import org.totalgrid.reef.client.exception.BadRequestException

import scala.collection.JavaConversions._
import org.totalgrid.reef.httpbridge.JsonBridgeConstants

class JsonFormatter extends SimpleFormatter {

  def mimeType = JsonBridgeConstants.JSON_FORMAT

  private val jsonFormatter = new JsonJacksonFormat

  def output[A <: Message](stream: ServletOutputStream, list: List[A]) {
    // build the json array of thre returned results
    val result = list.map { part =>
      jsonFormatter.printToString(part)
    }.mkString("{\"results\":[", ",", "]}")
    stream.print(result)
  }

  def outputOne[A <: Message](stream: ServletOutputStream, proto: A) {
    stream.print(jsonFormatter.printToString(proto))
  }

  def input[A <: Message.Builder](inputStream: InputStream, builder: A) {
    try {
      jsonFormatter.merge(inputStream, builder)

      // TODO: unknown fields are not working in current parser
      val unknownFields = builder.getUnknownFields.asMap().toMap
      if (unknownFields.size > 0) {
        throw new ServletException("Unknown fields in request: " + unknownFields)
      }

    } catch {
      case npe: NullPointerException =>
        // TODO: fix this in the parser code
        throw new BadRequestException("No JSON data in request")
      case ex: Exception =>
        throw new BadRequestException("Error parsing json data: " + ex.getMessage)
    }
  }
}