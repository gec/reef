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
package org.totalgrid.reef.integration.authz

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.totalgrid.reef.client.sapi.sync.AllScadaService
import org.totalgrid.reef.client.exception.UnauthorizedException
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.settings.UserSettings

class AuthTestBase extends ServiceClientSuite {

  override val modelFile = "../../assemblies/assembly-common/filtered-resources/samples/authorization/config.xml"

  var userConfig = Option.empty[UserSettings]

  override def beforeAll() {
    super.beforeAll()

    val props = PropertyReader.readFromFile("../../org.totalgrid.reef.test.cfg")
    userConfig = Some(new UserSettings(props))

    // update all of the agents to have the same system password
    client.getAgents().foreach { a =>
      client.setAgentPassword(a, userConfig.get.getUserPassword)
    }
  }

  /**
   * get a new client as a particular user (assumes password == username)
   */
  def as[A](userName: String, logout: Boolean = true)(f: AllScadaService => A): A = {
    val c = session.login(userName, userConfig.get.getUserPassword).await
    c.setHeaders(c.getHeaders.setResultLimit(5000))
    val ret = f(c.getRpcInterface(classOf[AllScadaService]))
    if (logout) c.logout()
    ret
  }

  /**
   * check that a call fails with an unauthorized error, just using intercept leads to useless error messages
   */
  def unAuthed(failureMessage: String)(f: => Unit) {
    try {
      f
      fail(failureMessage)
    } catch {
      case a: UnauthorizedException =>
      // were expecting the auth error, let others bubble
    }
  }
}
