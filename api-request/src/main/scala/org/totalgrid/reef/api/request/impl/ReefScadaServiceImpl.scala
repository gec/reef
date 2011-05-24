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
import org.totalgrid.reef.api.javaclient.{ Session, SessionExecutionPool }

/**
 * "Super" interface that includes all of the helpers for the individual services. This could be broken down
 * into smaller functionality based sections or not created at all.
 */
class AllScadaServicePooledWrapper(_sessionPool: SessionExecutionPool, _authToken: String)
    extends AllScadaService with AllScadaServiceImpl with AuthorizedAndPooledClientSource {
  def authToken = _authToken
  def sessionPool = _sessionPool
}

class AllScadaServiceWrapper(_session: Session) extends AllScadaService with AllScadaServiceImpl with SingleSessionClientSource {
  def session = convertByCasting(_session)
}

class AuthTokenServicePooledWrapper(_sessionPool: SessionExecutionPool) extends AuthTokenService with AuthTokenServiceImpl with PooledClientSource {
  def sessionPool = _sessionPool
}

class AuthTokenServiceWrapper(_session: Session) extends AuthTokenService with AuthTokenServiceImpl with SingleSessionClientSource {
  def session = convertByCasting(_session)
}

