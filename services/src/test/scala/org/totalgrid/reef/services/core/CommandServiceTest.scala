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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._
import org.totalgrid.reef.client.service.proto.Model.{ Command, CommandType, Entity }

@RunWith(classOf[JUnitRunner])
class CommandServiceTest extends DatabaseUsingTestBase {

  import SubscriptionTools._

  class Fixture {
    val fakeDatabase = new InMemoryMeasurementStore

    val contextSource = new MockContextSource(dbConnection)

    val modelFactories = new ModelFactories(new ServiceDependenciesDefaults(dbConnection, cm = fakeDatabase))
    val commandService = sync(new CommandService(modelFactories.cmds), contextSource)
    val entityService = sync(new EntityService(modelFactories.entities), contextSource)

    def addCommand(name: String = "cmd01", typ: CommandType = CommandType.CONTROL) = {
      val c = Command.newBuilder.setName(name).setDisplayName(name).setType(typ)
      commandService.put(c.build).expectOne
    }

    def getCommands(name: String = "*") = {
      val c = Command.newBuilder.setName(name)
      commandService.get(c.build).expectMany()
    }

    def getEntity(name: String = "cmd01") = {
      val e = Entity.newBuilder.setName(name).build
      entityService.get(e).expectOneOrNone()
    }

    def deleteCommand(name: String = "cmd01") = {
      val c = Command.newBuilder.setName(name)
      commandService.delete(c.build)
    }

    def events = contextSource.sink.events
  }

  test("Deleting point deletes dependent resources") {
    val f = new Fixture

    f.getCommands() should equal(Nil)
    f.getEntity() should equal(None)

    val cmd = f.addCommand()
    f.getCommands() should equal(cmd :: Nil)

    f.getEntity().isEmpty should equal(false)

    f.deleteCommand()

    f.getCommands() should equal(Nil)
    f.getEntity() should equal(None)

    val eventList = List(
      (ADDED, classOf[Entity]),
      (ADDED, classOf[Command]),
      (REMOVED, classOf[Command]),
      (REMOVED, classOf[Entity]))

    f.events.map(s => (s.typ, s.value.getClass)) should equal(eventList)
  }

}