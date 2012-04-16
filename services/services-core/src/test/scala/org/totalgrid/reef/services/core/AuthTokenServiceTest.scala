/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.Auth._
import org.totalgrid.reef.client.proto.Envelope._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import scala.collection.JavaConversions._

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.services.authz.SqlAuthzService
import org.totalgrid.reef.client.sapi.service.ServiceTypeIs
import org.totalgrid.reef.client.exception.{ UnauthorizedException, ReefServiceException }
import org.totalgrid.reef.services.framework.{ RequestContextSource, RequestContext }
import org.totalgrid.reef.services.{ HeadersContext, SilentRequestContext }
import org.totalgrid.reef.client.settings.Version
import org.totalgrid.reef.models.{ DatabaseUsingTestNotTransactionSafe, RunTestsInsideTransaction }
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders

class AuthSystemTestBase extends DatabaseUsingTestNotTransactionSafe with RunTestsInsideTransaction {

  override def beforeAll() {
    super.beforeAll()
    dbConnection.transaction {
      seedTesting(new SilentRequestContext)
    }
  }

  def seedTesting(context: RequestContext) {

    StandardAuthSeedData.seed(context, "system")

    val seeder = new StandardAuthSeedData.AuthSeeder(context, true)

    seeder.addUser("core", "core", List("all", "read_only"))
    seeder.addUser("operator", "operator", List("all", "read_only"))
    seeder.addUser("guest", "guest", List("read_only"))
  }

  class Fixture extends SubscriptionTools.SubscriptionTesting {
    def _dbConnection = dbConnection

    import SubscriptionTools._

    val modelFac = new ModelFactories(new ServiceDependenciesDefaults(dbConnection))

    val authService = new SyncService(new AuthTokenService(modelFac.authTokens), contextSource)

    val agentService = new SyncService(new AgentService(modelFac.agents), contextSource)
    val permissionSetService = new SyncService(new PermissionSetService(modelFac.permissionSets), contextSource)

    def loginFrom(user: String, location: String = "anywhere", version: String = Version.getClientVersion) = {
      login(user, user, None, None, location, version)
    }

    def login(
      user: String,
      pass: String,
      permissionSetName: Option[String] = None,
      timeoutAt: Option[Long] = None,
      location: String = "test code",
      version: String = Version.getClientVersion): AuthToken = {
      val agent = Agent.newBuilder.setName(user).setPassword(pass)

      val b = AuthToken.newBuilder.setAgent(agent).setLoginLocation(location).setClientVersion(version)
      permissionSetName.foreach(ps => b.addPermissionSets(PermissionSet.newBuilder.setName(ps)))
      timeoutAt.foreach(t => b.setExpirationTime(t))
      val authToken = authService.put(b.build).expectOne()
      // just check that the token is not a blank string
      authToken.getToken.length should not equal (0)
      authToken.getExpirationTime should (be >= System.currentTimeMillis)
      authToken
    }

    def checkRevoked(tokens: List[AuthToken], revokedState: Boolean) = {
      tokens.map { _.getRevoked } should equal(tokens.map { t => revokedState })
      tokens
    }

    def revoke(authToken: AuthToken) = {
      val revoked = authService.delete(authToken).expectOne()
      checkRevoked(List(revoked), true)
      revoked
    }
    def revokeOthers(authToken: AuthToken) = {
      val headers = BasicRequestHeaders.empty.setAuthToken(authToken.getToken)

      val request = AuthToken.newBuilder.setRevoked(false)

      checkRevoked(authService.delete(request.build, headers).expectMany(), true)
    }

    def revokeAgentsTokens(agentName: String) = {
      val request = AuthToken.newBuilder.setAgent(Agent.newBuilder.setName(agentName)).setRevoked(false)
      checkRevoked(authService.delete(request.build).expectMany(), true)
    }

    def permissionSets(authToken: AuthToken) = {
      authToken.getPermissionSetsList.toList.map(ps => ps.getName).sortWith((e1, e2) => (e1 compareTo e2) < 0)
    }

    def noTokens(list: List[AuthToken]): List[AuthToken] = {
      list.map { t => if (t.hasToken) fail("token visible on get") else t }
    }

    private def agent(name: String) = AuthToken.newBuilder.setAgent(Agent.newBuilder.setName(name))

    def searchByLoginLocation(login: String, count: Int) = {
      noTokens(authService.get(agent("*").setLoginLocation(login).build).expectMany(count))
    }
    def searchByAgent(name: String, count: Int) = {
      noTokens(authService.get(agent(name).build).expectMany(count))
    }
    def searchByVersion(name: String, count: Int) = {
      noTokens(authService.get(agent("*").setClientVersion(name).build).expectMany(count))
    }
    def searchByRevoked(revoked: Boolean, count: Int) = {
      noTokens(authService.get(agent("*").setRevoked(revoked).build).expectMany(count))
    }

    def getOwnTokens(count: Int) = {
      noTokens(authService.get(AuthToken.newBuilder.build).expectMany(count))
    }
  }
}

@RunWith(classOf[JUnitRunner])
class AuthSeederTest extends AuthSystemTestBase {
  test("Can update user permissions without changing password") {
    val fix = new Fixture
    val seeder = new StandardAuthSeedData.AuthSeeder(new SilentRequestContext, false)

    seeder.addUser("test-user", "test-pass", List("all"))

    val token = fix.login("test-user", "test-pass")
    token.getPermissionSets(0).getName should equal("all")

    // update the user with a different default password
    seeder.addUser("test-user", "different", List("read_only"))

    // password should be original value, permission set should be updated
    val token2 = fix.login("test-user", "test-pass")
    token2.getPermissionSets(0).getName should equal("read_only")
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
    authToken.getRevoked should equal(false)

    val deletedToken = fix.revoke(authToken)
    deletedToken.getRevoked should equal(true)
  }

  def sameIds(found: List[AuthToken], expected: List[AuthToken]) = {
    def getSorted(l: List[AuthToken]) = l.map { _.getId.getValue }.sorted

    getSorted(found) should equal(getSorted(expected))
  }

  test("Revoke all for agent") {
    val fix = new Fixture

    val core1 = fix.login("core", "core")
    val core2 = fix.login("core", "core")
    val core3 = fix.login("core", "core")

    val guest1 = fix.login("guest", "guest")
    val guest2 = fix.login("guest", "guest")

    sameIds(fix.revokeAgentsTokens("guest"), List(guest1, guest2))

    sameIds(fix.revokeAgentsTokens("core"), List(core1, core2, core3))
  }

  test("Revoke others for this agent") {
    val fix = new Fixture

    val core1 = fix.login("core", "core")
    val core2 = fix.login("core", "core")
    val core3 = fix.login("core", "core")

    fix.contextSource.userName = "core"
    sameIds(fix.revokeOthers(core1), List(core2, core3))

    sameIds(fix.getOwnTokens(3).filter(_.getRevoked == false), List(core1))
  }

}

@RunWith(classOf[JUnitRunner])
class AuthTokenQueryingTest extends AuthSystemTestBase {
  test("Login Searching") {
    val fix = new Fixture

    val authToken_hmi1 = fix.loginFrom("core", "hmi", "version1")
    val authToken_hmi2 = fix.loginFrom("core", "hmi", "version2")
    val authToken_mobile1 = fix.loginFrom("guest", "mobile", "version3")

    authToken_hmi1.getToken should not equal (authToken_hmi2.getToken)
    authToken_hmi1.getToken should not equal (authToken_mobile1.getToken)

    def agentsAre(tokens: List[AuthToken], name: String) {
      tokens.map { _.getAgent.getName }.distinct.head should equal(name)
    }
    def loginLocation(tokens: List[AuthToken], name: String) {
      tokens.map { _.getLoginLocation }.distinct.head should equal(name)
    }

    fix.searchByLoginLocation("*", 3)
    agentsAre(fix.searchByLoginLocation("hmi", 2), "core")
    agentsAre(fix.searchByLoginLocation("mobile", 1), "guest")
    fix.searchByLoginLocation("unknown", 0)

    fix.searchByAgent("*", 3)
    loginLocation(fix.searchByAgent("core", 2), "hmi")
    loginLocation(fix.searchByAgent("guest", 1), "mobile")
    fix.searchByAgent("unknown", 0)

    fix.searchByVersion("version1", 1)
    fix.searchByVersion("version2", 1)
    fix.searchByVersion("version3", 1)
    fix.searchByVersion("unknown", 0)

    fix.searchByRevoked(true, 0)
    fix.searchByRevoked(false, 3)

    fix.revoke(authToken_mobile1)

    agentsAre(fix.searchByRevoked(true, 1), "guest")
    agentsAre(fix.searchByRevoked(false, 2), "core")
  }

  test("Search for own auth token") {
    val fix = new Fixture

    fix.loginFrom("core", "hmi")
    fix.loginFrom("core", "mobile")
    fix.loginFrom("guest", "fep")
    fix.loginFrom("guest", "hmi")
    fix.loginFrom("guest", "mobile")

    fix.contextSource.userName = "core"
    fix.getOwnTokens(2)

    fix.contextSource.userName = "guest"
    fix.getOwnTokens(3)
  }
}

@RunWith(classOf[JUnitRunner])
class AuthTokenVerifierTest extends AuthSystemTestBase {

  class AuthRequestContext extends RequestContext with HeadersContext {

    def client = throw new Exception
    def eventSink = throw new Exception
    def operationBuffer = throw new Exception
    def subHandler = throw new Exception
    val auth = new SqlAuthzService()
  }

  class AuthTester extends ServiceTypeIs[Verb] {

    val componentId = "auth_tester"

    def request(verb: Verb, authTokens: List[String]) = {
      val context = new AuthRequestContext
      context.modifyHeaders { _.setAuthTokens(authTokens) }
      context.auth.prepare(context)
      verb match {
        case Verb.GET => context.auth.authorize(context, componentId, "read", Nil)
        case Verb.PUT => context.auth.authorize(context, componentId, "update", Nil)
        case Verb.POST => context.auth.authorize(context, componentId, "create", Nil)
        case Verb.DELETE => context.auth.authorize(context, componentId, "delete", Nil)
      }
    }
  }

  class AuthFixture extends Fixture {
    val test = new AuthTester

  }

  test("No AuthToken Attached => BadRequest") {
    val fix = new AuthFixture

    intercept[UnauthorizedException] {
      fix.test.request(Verb.GET, Nil)
    }
  }

  test("Faked AuthToken Attached => Unauthorized") {
    val fix = new AuthFixture

    intercept[UnauthorizedException] {
      fix.test.request(Verb.GET, List("fake-token"))
    }
  }

  test("Get w/ AuthToken => OK") {
    val fix = new AuthFixture
    val authToken = fix.login("guest", "guest")
    fix.test.request(Verb.GET, List(authToken.getToken))
  }

  test("Get w/ Revoked AuthToken => Unauthorized") {
    val fix = new AuthFixture

    val authToken = fix.login("guest", "guest")
    fix.authService.delete(authToken).expectOne()

    intercept[UnauthorizedException] {
      fix.test.request(Verb.GET, List(authToken.getToken))
    }
  }

  test("Put w/o Access => Unauthorized") {
    val fix = new AuthFixture

    val authToken = fix.login("guest", "guest")

    intercept[UnauthorizedException] {
      fix.test.request(Verb.PUT, List(authToken.getToken))
    }
  }

  test("Put w/ Access => OK") {
    val fix = new AuthFixture

    val authToken1 = fix.login("guest", "guest")
    val authToken2 = fix.login("core", "core")

    fix.test.request(Verb.PUT, List(authToken1.getToken, authToken2.getToken))
  }

}