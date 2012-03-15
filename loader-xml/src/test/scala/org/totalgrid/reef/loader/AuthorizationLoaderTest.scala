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
package org.totalgrid.reef.loader

import authorization.Authorization
import equipment.PointProfile
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.util.XMLHelper

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Auth.{ Agent, PermissionSet, Permission }

@RunWith(classOf[JUnitRunner])
class AuthorizationLoaderTest extends FunSuite with ShouldMatchers {

  def getXmlString(snippet: String) = """<?xml version="1.0" encoding="utf-8" standalone="yes"?>
    <authorization version="1.0"
      targetNamespace="equipment.loader.reef.totalgrid.org"
      xmlns="authorization.loader.reef.totalgrid.org">
    """ + snippet + "</authorization>"

  def getAuthObject(snip: String) = {
    XMLHelper.read(getXmlString(snip), classOf[Authorization])
  }

  case class PermCheck(allow: Boolean, actions: List[String], resources: List[String], selects: Option[List[String]] = None)

  def checkPerm(perm: Permission, check: PermCheck) {
    perm.getAllow should equal(check.allow)
    perm.getVerbList.toList should equal(check.actions)
    perm.getResourceList.toList should equal(check.resources)
  }

  def checkPermSet(set: PermissionSet, name: String, perms: List[PermCheck]) {
    set.getName should equal(name)
    set.getPermissionsCount should equal(perms.size)
    set.getPermissionsList.toList.zip(perms).foreach(tup => checkPerm(tup._1, tup._2))
  }

  test("Basic role") {
    val xml = """
      <roles>
          <role name="meas_reader">
              <allow actions="read" resources="point measurement" />
          </role>
      </roles>
        """

    val auth = getAuthObject(xml)
    val permList = AuthorizationLoader.mapRoles(auth)

    val check = List(PermCheck(true, List("read"), List("point", "measurement")))

    permList.size should equal(1)
    checkPermSet(permList.head, "meas_reader", check)
  }

  test("Multi perms") {
    val xml = """
      <roles>
          <role name="meas_reader">
              <allow actions="read" resources="point measurement" />
              <deny actions="update delete" resources="endpoint" />
          </role>
      </roles>
        """

    val auth = getAuthObject(xml)
    val permList = AuthorizationLoader.mapRoles(auth)

    val check = List(PermCheck(true, List("read"), List("point", "measurement")),
      PermCheck(false, List("update", "delete"), List("endpoint")))

    permList.size should equal(1)
    checkPermSet(permList.head, "meas_reader", check)
  }

  test("Multi role") {
    val xml = """
      <roles>
          <role name="role01">
              <allow actions="act01" resources="res01" />
          </role>
          <role name="role02">
              <deny actions="act02" resources="res02" />
          </role>
      </roles>
        """

    val auth = getAuthObject(xml)
    val permList = AuthorizationLoader.mapRoles(auth)

    val check1 = List(PermCheck(true, List("act01"), List("res01")))
    val check2 = List(PermCheck(false, List("act02"), List("res02")))

    permList.size should equal(2)
    checkPermSet(permList(0), "role01", check1)
    checkPermSet(permList(1), "role02", check2)
  }

  case class AgentCheck(name: String, password: String, permNames: List[String])

  def checkAgent(agent: Agent, check: AgentCheck) {
    agent.getName should equal(check.name)
    agent.getPassword should equal(check.password)
    agent.getPermissionSetsCount should equal(check.permNames.size)
    agent.getPermissionSetsList.toList.zip(check.permNames).foreach {
      case (proto, name) => proto.getName should equal(name)
    }
  }

  test("Basic agent") {
    val xml = """
      <agents>
            <agent name="agent01" roles="role01" />
      </agents>
        """

    val auth = getAuthObject(xml)
    val agentList = AuthorizationLoader.mapAgents(auth)

    agentList.size should equal(1)
    checkAgent(agentList.head, AgentCheck("agent01", "agent01", List("role01")))
  }

  test("Agent multi-role") {
    val xml = """
      <agents>
            <agent name="agent01" roles="role01 role02" />
      </agents>
        """

    val auth = getAuthObject(xml)
    val agentList = AuthorizationLoader.mapAgents(auth)

    agentList.size should equal(1)
    checkAgent(agentList.head, AgentCheck("agent01", "agent01", List("role01", "role02")))
  }

  test("Multi agent") {
    val xml = """
      <agents>
            <agent name="agent01" roles="role01" />
            <agent name="agent02" roles="role02" />
      </agents>
        """

    val auth = getAuthObject(xml)
    val agentList = AuthorizationLoader.mapAgents(auth)

    agentList.size should equal(2)
    checkAgent(agentList(0), AgentCheck("agent01", "agent01", List("role01")))
    checkAgent(agentList(1), AgentCheck("agent02", "agent02", List("role02")))
  }

}
