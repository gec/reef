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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.service.proto.Auth.AuthToken

@RunWith(classOf[JUnitRunner])
class LoginTest extends AuthTestBase {

  val ADMIN = "admin"

  test("Admin can see logins") {
    as(ADMIN) { admin =>
      val activeLogins = admin.getLogins(false)
      val allLogins = admin.getLogins(true)

      activeLogins.size should be <= allLogins.size

      val versions = allLogins.map { _.getClientVersion }.distinct
      val byVersion = versions.map { admin.getLoginsByClientVersion(true, _) }.flatten
      matchingIds(byVersion, allLogins)

      val ownLogins = admin.getOwnLogins(true)
      ownLogins.size should be > 0
      allAgent(ADMIN, ownLogins)

      val agents = admin.getAgents()

      agents.foreach { agent =>
        val logins = admin.getLoginsByAgent(true, agent.getName)
        allAgent(agent.getName, logins)
      }
    }
  }

  val GUEST = "guest"

  test("Guest can only see own logins") {
    (1 to 50).foreach { i => as(ADMIN) { c => } }

    as(GUEST) { guest =>
      guest.setHeaders(guest.getHeaders.setResultLimit(5))
      // we dont' get any guest logins
      val allLogins = guest.getLogins(true)
      allLogins should not equal (Nil)
      allAgent(GUEST, allLogins)

      val ownLogins = guest.getOwnLogins(true)
      allAgent(GUEST, ownLogins)
      matchingIds(allLogins, ownLogins)
    }
  }

  val USER = "user"

  test("User can revoke old logins") {
    as(USER, false) { user =>
      user.getOwnLogins(false).size should be > 0
    }
    val moreLogins = as(USER, false) { user =>
      user.getOwnLogins(false)
    }
    allRevoked(false, moreLogins)
    as(USER) { user =>
      // revoke all of the old logins
      val revoked = user.revokeOwnLogins()
      allAgent(USER, revoked)
      allRevoked(true, revoked)
      matchingIds(revoked, moreLogins)

      user.getOwnLogins(false).size should equal(1)
    }
  }

  private def allAgent(name: String, logins: List[AuthToken]) = {
    logins.map { _.getAgent.getName }.filterNot { _ == name } should equal(Nil)
    logins
  }
  private def allRevoked(state: Boolean, logins: List[AuthToken]) = {
    logins.map { _.getRevoked }.filterNot { _ == state } should equal(Nil)
    logins
  }
  private def matchingIds(l1: List[AuthToken], l2: List[AuthToken]) = {
    l1.map { _.getId.getValue }.sorted should equal(l2.map { _.getId.getValue }.sorted)
    l1
  }
}
