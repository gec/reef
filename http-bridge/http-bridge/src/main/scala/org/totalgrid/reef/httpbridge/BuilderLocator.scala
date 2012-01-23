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
package org.totalgrid.reef.httpbridge

import javax.servlet.http.HttpServletRequest
import org.totalgrid.reef.client.exception.BadRequestException
import com.google.protobuf.Message
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.ServicesList

import scala.collection.JavaConversions._

/**
 * encapsulates the logic to get a builder from a stringified name of the proto
 */
class BuilderLocator(servicesList: ServicesList) {

  // map of ids ("measurement", "user_command_request", etc) for rest endpoint names
  // could also use class.SimpleClassName for a CamelCase set of names
  private val idMap: Map[String, TypeDescriptor[_]] = servicesList.getServiceTypeInformation.toList.map { s => s.getDescriptor.id() -> s.getDescriptor }.toMap

  def getBuilder(req: HttpServletRequest) = {
    // assuming pathInfo is something like: /measurement_snapshot we get end of path which should be name of type
    val serviceType = Option(req.getPathInfo).getOrElse("").stripPrefix("/")
    val descriptor = idMap.get(serviceType).getOrElse(throw new BadRequestException("Unknown service type: " + serviceType + " valid types are: " + idMap.keys))

    val builder = descriptor.getKlass.getMethod("newBuilder").invoke(null).asInstanceOf[Message.Builder]
    builder
  }

  /**
   * return list of ids we support (for inspection)
   */
  def getIds = idMap.keys.toList
}