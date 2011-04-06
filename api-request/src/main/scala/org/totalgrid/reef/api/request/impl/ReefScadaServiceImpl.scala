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
import org.totalgrid.reef.api.javaclient.ISession
import org.totalgrid.reef.api.request._
import org.totalgrid.reef.api.scalaclient.{ ClientSession, SubscriptionManagement }

abstract class SessionWrapper(session: ISession) {
  protected val ops: ClientSession with SubscriptionManagement = session.getUnderlyingClient
}

/**
 * "Super" interface that includes all of the helpers for the individual services. This could be broken down
 * into smaller functionality based sections or not created at all.
 */
class ReefScadaServiceImpl(session: ISession) extends SessionWrapper(session) with AllScadaService with AllScadaServiceImpl

class AuthTokenServiceWrapper(session: ISession) extends SessionWrapper(session) with AuthTokenService with AuthTokenServiceImpl
class EntityServiceWrapper(session: ISession) extends SessionWrapper(session) with EntityService with EntityServiceImpl
class ConfigFileServiceWrapper(session: ISession) extends SessionWrapper(session) with ConfigFileService with ConfigFileServiceImpl
class MeasurementServiceWrapper(session: ISession) extends SessionWrapper(session) with MeasurementService with MeasurementServiceImpl
class MeasurementOverrideServiceWrapper(session: ISession) extends SessionWrapper(session) with MeasurementOverrideService with MeasurementOverrideServiceImpl
class EventServiceWrapper(session: ISession) extends SessionWrapper(session) with EventService with EventServiceImpl
class CommandServiceWrapper(session: ISession) extends SessionWrapper(session) with CommandService with CommandServiceImpl
class PointServiceWrapper(session: ISession) extends SessionWrapper(session) with PointService with PointServiceImpl
class AlarmServiceWrapper(session: ISession) extends SessionWrapper(session) with AlarmService with AlarmServiceImpl
