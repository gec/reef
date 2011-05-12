package org.totalgrid.reef.api.request.impl

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.totalgrid.reef.api.request._
import org.totalgrid.reef.api.javaclient.{ ISession, ISessionPool }

abstract class AuthorizedSessionWrapper(_sessionPool: ISessionPool, _authToken: String) extends AuthorizedAndPooledClientSource {
  def authToken = _authToken
  def sessionPool = _sessionPool
}

abstract class PooledSessionWrapper(_sessionPool: ISessionPool) extends PooledClientSource {
  def sessionPool = _sessionPool
}

abstract class SingleSessionWrapper(_session: ISession) extends SingleSessionClientSource {
  def session = _session.getUnderlyingClient
}

/**
 * "Super" interface that includes all of the helpers for the individual services. This could be broken down
 * into smaller functionality based sections or not created at all.
 */
class AllScadaServicePooledWrapper(sessionPool: ISessionPool, authToken: String)
  extends AuthorizedSessionWrapper(sessionPool, authToken) with AllScadaService with AllScadaServiceImpl

class AllScadaServiceWrapper(session: ISession) extends SingleSessionWrapper(session) with AllScadaService with AllScadaServiceImpl

class AuthTokenServicePooledWrapper(sessionPool: ISessionPool)
  extends PooledSessionWrapper(sessionPool) with AuthTokenService with AuthTokenServiceImpl

class AuthTokenServiceWrapper(session: ISession) extends SingleSessionWrapper(session) with AuthTokenService with AuthTokenServiceImpl

