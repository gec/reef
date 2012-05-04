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
package org.totalgrid.reef.client.sapi.client.rest

import org.totalgrid.reef.client.Promise
import org.totalgrid.reef.client.settings.UserSettings

trait Login {

  def login(authToken: String): Client
  def login(userName: String, password: String): Promise[Client]
  def login(userSettings: UserSettings): Promise[Client]

}

trait Logout {
  def logout(authToken: String): Promise[Boolean]
  def logout(client: Client): Promise[Boolean]
}

trait ClientLogout {
  def logout(): Promise[Boolean]
}