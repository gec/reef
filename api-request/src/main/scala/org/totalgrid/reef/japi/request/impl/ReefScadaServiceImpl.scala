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
package org.totalgrid.reef.japi.request.impl

import org.totalgrid.reef.sapi.request._
import org.totalgrid.reef.japi.request.AllScadaService
import org.totalgrid.reef.japi.client.{ SubscriptionCreationListener, Session, SessionExecutionPool }
import org.totalgrid.reef.sapi.client.{ ClientSession, SubscriptionManagement, RestOperations, SessionPool }

/**
 * "Super" interface that includes all of the helpers for the individual services. This could be broken down
 * into smaller functionality based sections or not created at all.
 */
class AllScadaServicePooledWrapper(scalaClient: AllScadaServiceImpl)
    extends AllScadaService with AllScadaServiceJavaShim {

  def this(pool: SessionExecutionPool, authToken: String) = this(new AllScadaServiceExecutionPool(pool, authToken))

  def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = scalaClient.addSubscriptionCreationListener(listener)

  def service = scalaClient
}

class AllScadaServiceExecutionPool(_sessionPool: SessionExecutionPool, _authToken: String)
    extends AllScadaServiceImpl with AuthorizedAndPooledClientSource {
  def authToken = _authToken
  def sessionPool = _sessionPool
}

class AllScadaServicePooled(sessionPool: SessionPool, authToken: String)
    extends AllScadaServiceImpl with ClientSource {

  override def _ops[A](block: RestOperations with SubscriptionManagement => A): A = {
    sessionPool.borrow(authToken)(block)
  }
}

class AllScadaServiceScalaSingleSession(_session: ClientSession)
    extends AllScadaServiceImpl with SingleSessionClientSource {

  def session = _session
}

class AllScadaServiceSingleSession(scalaClient: AllScadaServiceScalaSingleSession)
    extends AllScadaService with AllScadaServiceJavaShim {

  def this(session: ClientSession) = this(new AllScadaServiceScalaSingleSession(session))

  def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = scalaClient.addSubscriptionCreationListener(listener)

  def service = scalaClient

  def session = scalaClient.session
}

