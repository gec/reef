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
package org.totalgrid.reef.models

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }

@RunWith(classOf[JUnitRunner])
class ApplicationSchemaTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {

  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }

  override def beforeEach() {
    transaction { ApplicationSchema.reset }
  }

  test("Link Application and Heatbeat tables") {
    ApplicationSchema.agents.Count.head should equal(0)
    ApplicationSchema.heartbeats.Count.head should equal(0)
    ApplicationSchema.apps.Count.head should equal(0)

    val app1 = ApplicationSchema.apps.insert(new ApplicationInstance("fep01", "client", "any", "any"))
    val app2 = ApplicationSchema.apps.insert(new ApplicationInstance("fep02", "client", "any", "any"))
    val hbeat1 = ApplicationSchema.heartbeats.insert(new HeartbeatStatus(app1.id, 1000, 0, false, "deadman"))

    hbeat1.application.value should equal(app1)

    ApplicationSchema.heartbeats.where(h => h.applicationId === app1.id).single should equal(hbeat1)

    app1.heartbeat.value should equal(hbeat1)

    ApplicationSchema.heartbeats.Count.head should equal(1)
    ApplicationSchema.apps.Count.head should equal(2)
  }

  //  test("Get Points and Commands From Node") {
  //    val node1 = ApplicationSchema.logicalNodes.insert(new LogicalNode("fep01"))
  //    val node2 = ApplicationSchema.logicalNodes.insert(new LogicalNode("fep02"))
  //    for (i <- 1 to 100) yield ApplicationSchema.points.insert(new Point("point" + i, 0, node1.id))
  //    for (i <- 1 to 50) yield ApplicationSchema.commands.insert(new Command("cmd" + i, 0, node1.id))
  //
  //    node1.points.size should equal(100)
  //    node1.commands.size should equal(50)
  //
  //    node2.points.size should equal(0)
  //  }

}