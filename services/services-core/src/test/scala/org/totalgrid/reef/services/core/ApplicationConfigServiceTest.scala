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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.client.service.proto.Application._
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.models.DatabaseUsingTestBase

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._
import org.totalgrid.reef.client.service.proto.Model.{ Entity, ReefUUID }
import org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot
import org.totalgrid.reef.client.sapi.client.Expectations._

@RunWith(classOf[JUnitRunner])
class ApplicationConfigServiceTest extends DatabaseUsingTestBase {

  class Fixture extends SubscriptionTools.SubscriptionTesting {
    def _dbConnection = dbConnection
    val modelFac = new ModelFactories(new ServiceDependenciesDefaults(dbConnection))
    val service = new SyncService(new ApplicationConfigService(modelFac.appConfig), contextSource)
  }

  test("GetPutDelete") {
    val f = new Fixture

    val b = ApplicationConfig.newBuilder
      .setUserName("fep")
      .setInstanceName("fep01")
      .setNetwork("any")
      .setLocation("farm1")
      .addCapabilites("FEP")

    f.service.get(ApplicationConfig.newBuilder().setUuid(ReefUUID.newBuilder.setValue("*")).build).expectNone()

    f.service.put(b.build).expectOne(Status.CREATED)

    f.service.get(ApplicationConfig.newBuilder().setUuid(ReefUUID.newBuilder.setValue("*")).build).expectOne()
    val config1 = f.service.get(ApplicationConfig.newBuilder().setInstanceName("fep01").build).expectOne()
    val updated = f.service.put(config1.toBuilder.setLocation("farm2").build).expectOne(Status.UPDATED)
    updated.getLocation should equal("farm2")

    f.service.put(updated).expectOne(Status.NOT_MODIFIED)
    val config2 = f.service.get(ApplicationConfig.newBuilder().setUuid(ReefUUID.newBuilder.setValue("*")).build).expectOne()
    config2.getLocation should equal("farm2")

    f.service.delete(config2).expectOne(Status.DELETED)
    f.service.get(ApplicationConfig.newBuilder().setUuid(ReefUUID.newBuilder.setValue("*")).build).expectNone()

    val eventList = List(
      (ADDED, classOf[Entity]),
      (ADDED, classOf[ApplicationConfig]),
      (ADDED, classOf[StatusSnapshot]),
      (MODIFIED, classOf[ApplicationConfig]),
      (REMOVED, classOf[StatusSnapshot]),
      (REMOVED, classOf[ApplicationConfig]),
      (REMOVED, classOf[Entity]))

    f.eventCheck should equal(eventList)
  }
}