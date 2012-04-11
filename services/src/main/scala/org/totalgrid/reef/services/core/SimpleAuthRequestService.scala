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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.proto.SimpleAuth.AuthRequest
import org.totalgrid.reef.services.framework.{ RequestContextSource, ServiceEntryPoint }
import org.totalgrid.reef.client.sapi.client.Response
import org.totalgrid.reef.client.service.proto.Auth.{ Agent, AuthToken }

import org.totalgrid.reef.client.sapi.types.BuiltInDescriptors
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.ReefServiceException
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.services.Version

class SimpleAuthRequestService(protected val model: AuthTokenServiceModel)
    extends ServiceEntryPoint[AuthRequest] with Logging {
  override val descriptor = BuiltInDescriptors.authRequest()

  override def postAsync(contextSource: RequestContextSource, req: AuthRequest)(callback: (Response[AuthRequest]) => Unit) {
    val builder = AuthToken.newBuilder.setAgent(Agent.newBuilder.setName(req.getName).setPassword(req.getPassword))
    if (req.hasClientVersion) builder.setClientVersion(req.getClientVersion)
    val authTokenRecord = contextSource.transaction { model.createFromProto(_, builder.build()) }
    val response = req.toBuilder.setToken(authTokenRecord.token).setServerVersion(Version.getClientVersion).build()
    callback(Response(Envelope.Status.OK, response))
  }

  override def deleteAsync(source: RequestContextSource, req: AuthRequest)(callback: (Response[AuthRequest]) => Unit) {
    val proto = AuthToken.newBuilder.setToken(req.getToken).build
    try {
      source.transaction { context =>
        val existing = model.findRecords(context, proto)
        existing.foreach(a =>
          model.delete(context, a))

      }
    } catch {
      // we don't want to expose to the user if they actually deleted a token as that reveals too much
      // information about the system and complicates shutdown code.
      case rse: ReefServiceException => logger.info("Error deleting auth token: " + rse.getMessage)
    }
    callback(Response(Envelope.Status.DELETED, req.toBuilder.setToken(req.getToken).build))
  }
}