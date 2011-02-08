/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.FEP.{ CommunicationEndpointConfig => ConfigProto, CommunicationEndpointConnection => ConnProto }

import scala.collection.JavaConversions._

trait FEPOperations {

  val services: FrontEndServices

  def loadOrThrow(conn: ConnProto): ConnProto = {

    val cp = ConnProto.newBuilder(conn)

    val ep = services.config.getOneOrThrow(conn.getEndpoint)
    val endpoint = ConfigProto.newBuilder(ep)

    ep.getConfigFilesList.toList.foreach(cf => endpoint.addConfigFiles(services.file.getOneOrThrow(cf)))

    if (ep.hasPort) endpoint.setPort(services.port.getOneOrThrow(ep.getPort))
    cp.setEndpoint(endpoint).build()
  }

}
