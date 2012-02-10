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
import org.totalgrid.reef.httpbridge.servlets.helpers.ServletBase
import org.totalgrid.reef.httpbridge.BuilderLocator
import org.totalgrid.reef.client.service.proto.Model.Entity

/**
 * simple servlet that will translate between different formats of the POSTed proto using the
 * mimetypes mentioned in the Content-Type and Accept headers.
 *
 * It returns its result as single entry (not a list) so it can be put directly back into
 * another
 */
class ConverterServlet(builderLocator: BuilderLocator) extends ServletBase {
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {

    val builder = builderLocator.getBuilder(req)
    val requestProto = parseInput(req, req.getInputStream, builder).build()

    printSingleOutput(req, resp, requestProto)
    resp.setStatus(200)
  }

  /**
   * return a specific proto's descriptor or a list of supported types
   */
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {

    import scala.collection.JavaConversions._

    val responseProto = if (Option(req.getPathInfo).getOrElse("").stripPrefix("/") == "") {
      val ids = builderLocator.getIds

      // TODO: replace entity types with custom proto
      Entity.newBuilder().addAllTypes(ids).build
    } else {
      val builder = builderLocator.getBuilder(req)

      builder.getDescriptorForType.toProto
    }

    printSingleOutput(req, resp, responseProto)
    resp.setStatus(200)
  }
}