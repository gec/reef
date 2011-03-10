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
package org.totalgrid.reef.api.request

import org.totalgrid.reef.api.javaclient.ISession

/**
 * "Super" interface that includes all of the helpers for the individual services. This could be broken down
 * into smaller functionality based sections or not created at all.
 */
trait AllScadaHelpers extends AuthTokenHelpers with EntityHelpers with ConfigFileHelpers

/**
 * "Super" implementation of the the super interface that includes all of the implementations, again maybe not needed
 */
trait AllScadaHelpersImpl extends AllScadaHelpers with AuthTokenHelpersImpl with EntityHelpersImpl with ConfigFileHelpersImpl

/**
 * base class that java clients create to use all of the api helper functionality. Designed to be overloaded so client
 * apps can add functionality that isn't provided in the core api functions.
 */
class ReefScadaHelpersImpl(session: ISession) extends AllScadaHelpers with AllScadaHelpersImpl {
  protected val ops = session.getUnderlyingClient
}

/**
 * An alternative to the ReefScadaHelpersImpl version that combines all of the functionality under one roof, shows
 * implementation of functionality is flexible.
 */
class AllScadaHelpersContainer(session : ISession){
  lazy val authToken = new AuthTokenHelpersWrapper(session)
  lazy val entities = new EntityHelpersWrapper(session)
  lazy val configFiles = new ConfigFileHelpersWrapper(session)
}

/**
 * functionality can also be wrapped and used on an interface basis. This would allow us to keep the functionality
 * partioned by service type/ functionality if we wanted
 */
class AuthTokenHelpersWrapper(session: ISession) extends AuthTokenHelpers with AuthTokenHelpersImpl {
  protected val ops = session.getUnderlyingClient
}

class EntityHelpersWrapper(session: ISession) extends EntityHelpers with EntityHelpersImpl {
  protected val ops = session.getUnderlyingClient
}

class ConfigFileHelpersWrapper(session: ISession) extends ConfigFileHelpers with ConfigFileHelpersImpl {
  protected val ops = session.getUnderlyingClient
}

