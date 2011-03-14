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

import org.totalgrid.reef.proto.Model.{ Entity, EntityAttributes }
import org.totalgrid.reef.proto.Utils.Attribute

import org.totalgrid.reef.api.Envelope.Status

import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models.RunTestsInsideTransaction
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

@RunWith(classOf[JUnitRunner])
class EntityAttributesServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {

  override def beforeAll() = DbConnector.connect(DbInfo.loadInfo("test"))

  override def beforeEach() = transaction { ApplicationSchema.reset }

  //test ("TestTest") { println("test")}

  /*test("Create") {

    val service = new EntityAttributesService

    val entity = Entity.newBuilder.setUid("1").build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build
    one(Status.CREATED, service.put(entAttr))
  }*/
}