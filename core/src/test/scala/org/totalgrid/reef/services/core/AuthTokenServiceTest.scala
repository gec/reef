/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.proto.Auth._
import org.totalgrid.reef.api.Envelope._
import org.totalgrid.reef.api.service.NoOpService
import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models.RunTestsInsideTransaction
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import scala.collection.JavaConversions._

import org.totalgrid.reef.services.{ AuthTokenVerifier, AuthTokenMetrics }

import org.totalgrid.reef.services.ServiceProviderHeaders._

import org.totalgrid.reef.messaging.serviceprovider.SilentEventPublishers
import org.totalgrid.reef.api.{ ReefServiceException, RequestEnv }
import org.totalgrid.reef.api.service.IServiceResponseCallback

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

class AuthSystemTestBase extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {

  override def beforeAll() = DbConnector.connect(DbInfo.loadInfo("test"))
  override def beforeEach() = {
    transaction { ApplicationSchema.reset }
    transaction { AuthTokenService.seed() }
  }

  class Fixture {
    val modelFac = new ModelFactories(new SilentEventPublishers, new SilentSummaryPoints)
    val service = new AuthTokenService(modelFac.authTokens)

    def loginFrom(user: String, location: String) = {
      login(user, user, None, None, location)
    }

    def login(user: String, pass: String, permissionSetName: Option[String] = None, timeoutAt: Option[Long] = None, location: String = "test code"): AuthToken = {
      val agent = Agent.newBuilder.setName(user).setPassword(pass)

      val b = AuthToken.newBuilder.setAgent(agent).setLoginLocation(location)
      permissionSetName.foreach(ps => b.addPermissionSets(PermissionSet.newBuilder.setName(ps)))
      timeoutAt.foreach(t => b.setExpirationTime(t))
      val authToken = one(service.put(b.build))
      // just check that the token is not a blank string
      authToken.getToken.length should not equal (0)
      authToken.getExpirationTime should (be >= System.currentTimeMillis)
      authToken
    }

    def permissionSets(authToken: AuthToken) = {
      authToken.getPermissionSetsList.toList.map(ps => ps.getName).sortWith((e1, e2) => (e1 compareTo e2) < 0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
class AuthTokenServiceTest extends AuthSystemTestBase {

  test("Bad User") {
    val fix = new Fixture
    intercept[ReefServiceException] {
      fix.login("baduser", "badpass")
    }
  }

  test("Wrong Password") {
    val fix = new Fixture
    intercept[ReefServiceException] {
      fix.login("core", "badpass")
    }
  }

  test("Get Auth Token with 2 permission sets") {
    val fix = new Fixture

    val authToken = fix.login("core", "core")

    fix.permissionSets(authToken) should equal(List("all", "read_only"))
  }

  test("Get AuthToken with permission sets name *") {
    val fix = new Fixture

    val authToken = fix.login("core", "core", Some("*"))

    fix.permissionSets(authToken) should equal(List("all", "read_only"))
  }

  test("Get all permissions for read_only user") {
    val fix = new Fixture

    val authToken = fix.login("guest", "guest")

    fix.permissionSets(authToken) should equal(List("read_only"))
  }

  test("Try to get unallowed permsissions") {
    val fix = new Fixture

    intercept[ReefServiceException] {
      // we only have access to the "read_only" permission
      fix.login("guest", "guest", Some("all"))
    }
  }

  test("Get read_only subset of permissions") {
    val fix = new Fixture

    val authToken = fix.login("core", "core", Some("read_only"))

    fix.permissionSets(authToken) should equal(List("read_only"))
  }

  test("Specify when permissions will timeout") {
    val fix = new Fixture

    val time = System.currentTimeMillis + 5000

    val authToken = fix.login("core", "core", None, Some(time))

    authToken.getExpirationTime should equal(time)
  }

  test("Try to specify past timeout") {
    val fix = new Fixture

    val time = System.currentTimeMillis - 5000

    intercept[ReefServiceException] {
      fix.login("core", "core", None, Some(time))
    }
  }

  test("Revoke AuthToken") {
    val fix = new Fixture

    val authToken = fix.login("core", "core")
    val deletedToken = one(fix.service.delete(authToken))

    deletedToken.getExpirationTime should equal(-1)
  }

  test("Multiple Logins") {
    val fix = new Fixture

    val authToken_hmi1 = fix.loginFrom("core", "hmi")
    val authToken_hmi2 = fix.loginFrom("core", "hmi")
    val authToken_mobile1 = fix.loginFrom("core", "mobile")

    authToken_hmi1.getToken should not equal (authToken_hmi2.getToken)
    authToken_hmi1.getToken should not equal (authToken_mobile1.getToken)

  }
}

@RunWith(classOf[JUnitRunner])
class AuthTokenVerifierTest extends AuthSystemTestBase {
  class AuthFixture extends Fixture {
    val wrappedService = new AuthTokenVerifier(new NoOpService, "test", new AuthTokenMetrics)

    /// make a request with the set verb and auth_tokens
    def makeRequest(verb: Verb, authTokens: List[String]) = {
      val req = ServiceRequest.newBuilder.setId("magic")
      req.setVerb(verb)
      // simple proto we can build for the payload; not used for anything
      req.setPayload(ServiceResponse.newBuilder.setId("").setStatus(Status.BUS_UNAVAILABLE).build.toByteString)
      req.build
    }

    def testRequest(status: Status, verb: Verb, authTokens: List[String]) = {
      val env = new RequestEnv
      env.setAuthTokens(authTokens)
      val callback = new IServiceResponseCallback {
        var response: Option[ServiceResponse] = None
        def onResponse(rsp: ServiceResponse) = response = Some(rsp)
      }
      wrappedService.respond(makeRequest(verb, authTokens), env, callback)
      callback.response.get.getStatus should equal(status)
    }
  }

  test("No AuthToken Attached => BadRequest") {
    val fix = new AuthFixture

    fix.testRequest(Status.BAD_REQUEST, Verb.GET, Nil)
  }

  test("Faked AuthToken Attached => Unauthorized") {
    val fix = new AuthFixture

    fix.testRequest(Status.UNAUTHORIZED, Verb.GET, List("fake-token"))
  }

  test("Get w/ AuthToken => OK") {
    val fix = new AuthFixture

    val authToken = fix.login("guest", "guest")

    fix.testRequest(Status.OK, Verb.GET, List(authToken.getToken))
  }

  test("Get w/ Revoked AuthToken => Unauthorized") {
    val fix = new AuthFixture

    val authToken = fix.login("guest", "guest")
    one(fix.service.delete(authToken))

    fix.testRequest(Status.UNAUTHORIZED, Verb.GET, List(authToken.getToken))
  }

  test("Put w/o Access => Unauthorized") {
    val fix = new AuthFixture

    val authToken = fix.login("guest", "guest")

    fix.testRequest(Status.UNAUTHORIZED, Verb.PUT, List(authToken.getToken))
  }

  test("Put w/ Access => OK") {
    val fix = new AuthFixture

    val authToken1 = fix.login("guest", "guest")
    val authToken2 = fix.login("core", "core")

    fix.testRequest(Status.OK, Verb.PUT, List(authToken1.getToken, authToken2.getToken))
  }

}