/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.impl

import net.agileautomata.executor4s.{ Strand, Executor }
import org.totalgrid.reef.client.operations.impl.DefaultServiceOperations
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.client.settings.Version
import org.totalgrid.reef.client.proto.SimpleAuth.AuthRequest
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.{ RequestHeaders, Promise }

abstract class ClientLogin(requests: RequestSender, executor: Executor) extends Logging {

  def createClient(headers: RequestHeaders, strand: Strand): ClientImpl

  def login(userName: String, password: String): Promise[ClientImpl] = {
    val strand = Strand(executor)
    DefaultServiceOperations.safeOp("Error logging in with name: " + userName, strand) {
      val agent = AuthRequest.newBuilder.setName(userName).setPassword(password).setClientVersion(Version.getClientVersion).build
      def convert(r: AuthRequest): ClientImpl = {
        if (!r.hasServerVersion) logger.warn("Login response did not include the server version")
        else if (r.getServerVersion != Version.getClientVersion) {
          logger.warn("The server is running " + r.getServerVersion + ", but the client is " + Version.getClientVersion)
        }
        createClient(BasicRequestHeaders.fromAuth(r.getToken), strand)
      }
      requests.request(Envelope.Verb.POST, agent, BasicRequestHeaders.empty, strand).map(_.one).map(convert)
    }
  }

  def logout(authToken: String, strand: Executor): Promise[Boolean] = {
    DefaultServiceOperations.safeOp("Error revoking auth token.", strand) {
      val agent = AuthRequest.newBuilder.setToken(authToken).build
      val headers = BasicRequestHeaders.empty.setAuthToken(authToken)
      requests.request(Envelope.Verb.DELETE, agent, headers, strand).map(_.one).map(e => true)
    }
  }
}

