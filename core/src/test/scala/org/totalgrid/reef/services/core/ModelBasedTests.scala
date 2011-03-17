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

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.models.RunTestsInsideTransaction

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.Model.{ Entity => EntityProto, Relationship }
import org.totalgrid.reef.proto.Model.{ Point => PointProto }
import org.totalgrid.reef.models._
import org.totalgrid.reef.services._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.messaging.serviceprovider.SilentEventPublishers

@RunWith(classOf[JUnitRunner])
class ModelBasedTests extends FunSuite with ShouldMatchers with BeforeAndAfterAll with RunTestsInsideTransaction {
  override def beforeAll() {
    import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
    DbConnector.connect(DbInfo.loadInfo("test"))
    transaction {
      ApplicationSchema.reset
    }
    transaction {
      ModelSeed.seed()
      seedPoints
    }
  }

  def seedPoints {
    EQ.findEntitiesByType(List("Point")).foreach { ent =>
      ApplicationSchema.points.insert(new Point(ent.name, ent.id, false))
    }
  }

  test("Point model lookup") {
    val pubs = new SilentEventPublishers
    val models = new ModelFactories(pubs, new SilentSummaryPoints)
    val service = new PointService(models.points)

    val entReq =
      EntityProto.newBuilder
        .setName("Pittsboro")
        .addTypes("Substation")
        .addRelations(
          Relationship.newBuilder
            .setRelationship("owns")
            .setDescendantOf(true)
            .addEntities(
              EntityProto.newBuilder
                .addTypes("Bus")
                .addRelations(Relationship.newBuilder
                  .setRelationship("owns")
                  .setDescendantOf(true)
                  .addEntities(
                    EntityProto.newBuilder
                      .addTypes("Point"))))).build

    val req = PointProto.newBuilder.setEntity(entReq).build
    val specIds = ApplicationSchema.points.where(t => t.name === "Pittsboro.B12.Kv" or t.name === "Pittsboro.B24.Kv").map(_.id).toList
    val resp = service.get(req)
    val resultIds = resp.map(_.getUid.toLong)

    specIds.foldLeft(resultIds) { (left, id) =>
      left.contains(id) should equal(true)
      left.filterNot(_ == id)
    } should equal(Nil)
  }

}
