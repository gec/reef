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
package org.totalgrid.reef.models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FunSuite }
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector }
import org.totalgrid.reef.services.ServiceBootstrap
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.squeryl.PrimitiveTypeMode._
import scala.collection.mutable.ListBuffer

import org.totalgrid.reef.services.core.SyncServiceShims._
import org.totalgrid.reef.services.core.AlarmQueryService
import org.totalgrid.reef.api.proto.Alarms._
import org.totalgrid.reef.api.proto.Events._

@RunWith(classOf[JUnitRunner])
class EventAlarmIndexingTests extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach {

  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("../org.totalgrid.reef.test.cfg"))
    ServiceBootstrap.resetDb
  }

  test("Seed events") {

    val entCount = transaction {
      from(ApplicationSchema.entities)(e =>
        compute(count())).toInt
    }

    val numEnts = 20
    if (entCount == 0) {

      var i = 0
      val ents = new ListBuffer[Entity]
      while (i < numEnts) {
        ents += new Entity("ent" + i)
        i = i + 1
      }

      transaction {
        ApplicationSchema.entities.insert(ents.toList)
        val entIds = from(ApplicationSchema.entities)(e => select(e.id)).toList
        ApplicationSchema.entityTypes.insert(entIds.map(new EntityToTypeJoins(_, "type01")))
      }
    }

    val cnt = transaction {
      from(ApplicationSchema.events)(e =>
        compute(count())).toInt
    }

    val chunkSize = 100
    val chunks = 1

    if (cnt == 0) {
      val userCount = 10
      val typeCount = 8
      val subCount = 20

      var typ: Long = 0
      var user: Long = 0
      var sub: Long = 0
      var time: Long = 0

      val entities = transaction {
        from(ApplicationSchema.entities)((e) => select(e)).toList
      }

      val entIds = entities.map(_.id).toArray

      var j = 0
      while (j < chunks) {
        val l = new ListBuffer[EventStore]

        transaction {
          var i = 0
          while (i < chunkSize) {
            val typName = "type" + (typ % typeCount)
            val userName = "user" + (user % userCount)
            val subName = "sub" + (sub % subCount)

            val entId = Some(entIds(i % entIds.size))

            val ev = EventStore(typName, false, time, None, 4, subName, userName, entId, new Array[Byte](0), "event render")
            l += ev

            i = i + 1
            typ = typ + 1
            user = user + 1
            sub = sub + 1
            time = time + 1
          }

          ApplicationSchema.events.insert(l.toList)

          j = j + 1
        }
      }
    }

    val cntAlarm = transaction {
      from(ApplicationSchema.alarms)(e =>
        compute(count())).toInt
    }

    if (cntAlarm == 0) {
      val stateCount = 4
      var state: Int = 0

      var j = 0
      while (j < chunks) {

        transaction {

          val events = from(ApplicationSchema.events)((e) =>
            select(e)).page(j, chunkSize).toList

          val alarms = events.map { e =>
            val a = new AlarmModel((state % stateCount) + 1, e.id)
            state = state + 1
            a
          }

          ApplicationSchema.alarms.insert(alarms)
        }

        j = j + 1
      }
    }

  }

  test("Query1") {
    val alarms = ApplicationSchema.alarms
    val events = ApplicationSchema.events

    val alarmQueryService = new AlarmQueryService

    val query = AlarmList.newBuilder.setSelect(
      AlarmSelect.newBuilder.setEventSelect(
        EventSelect.newBuilder
          .addEventType("type1")
          .setLimit(100)))

    alarmQueryService.get(query.build)

  }
}