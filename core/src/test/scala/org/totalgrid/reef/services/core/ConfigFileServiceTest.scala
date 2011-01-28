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

import org.totalgrid.reef.proto.Model.ConfigFile
import org.totalgrid.reef.proto.Model.Entity
import org.totalgrid.reef.proto.Envelope.Status

import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models.RunTestsInsideTransaction
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.SilentEventPublishers
import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

@RunWith(classOf[JUnitRunner])
class ConfigFileServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {

  override def beforeAll() = DbConnector.connect(DbInfo.loadInfo("test"))

  override def beforeEach() = transaction { ApplicationSchema.reset }

  def makeConfigFile(name: String, mime: String, data: String, owner: Option[Entity] = None) = {
    import org.totalgrid.reef.messaging.ProtoSerializer.convertStringToByteString
    val cfb = ConfigFile.newBuilder.setName(name).setMimeType(mime).setFile(data)
    owner.foreach(cfb.setEntity(_))
    cfb.build
  }

  test("Test Status Codes") {
    val publisher = new SilentEventPublishers

    val s = new ConfigFileService(new ConfigFileServiceModelFactory(publisher))

    val configFile = makeConfigFile("testFile1", "text", "blah")

    val result = one(Status.CREATED, s.put(configFile))

    one(Status.NOT_MODIFIED, s.put(configFile)).getUid should equal(result.getUid)

    val configFileMod = makeConfigFile("testFile1", "text", "im different!")
    one(Status.UPDATED, s.put(configFileMod)).getUid should equal(result.getUid)

    val configFileMod2 = makeConfigFile("testFile1", "mimediff", "im different!")
    val finalObject = one(Status.UPDATED, s.put(configFileMod2))

    one(Status.DELETED, s.delete(finalObject)).getUid should equal(result.getUid)
  }

  test("Test Searching") {
    val publisher = new SilentEventPublishers

    val s = new ConfigFileService(new ConfigFileServiceModelFactory(publisher))

    val configFile1 = makeConfigFile("testFile1", "text", "blah")
    val configFile2 = makeConfigFile("testFile2", "text", "blah")
    val configFile3 = makeConfigFile("testFile3", "html", "blah")
    val configFile4 = makeConfigFile("testFile4", "html", "blah")

    one(Status.CREATED, s.put(configFile1))
    one(Status.CREATED, s.put(configFile2))
    one(Status.CREATED, s.put(configFile3))
    one(Status.CREATED, s.put(configFile4))

    many(4, s.get(ConfigFile.newBuilder.setUid("*").build))
    many(2, s.get(ConfigFile.newBuilder.setMimeType("text").build))
    many(2, s.get(ConfigFile.newBuilder.setMimeType("html").build))
    many(0, s.get(ConfigFile.newBuilder.setMimeType("xml").build))

    many(1, s.get(ConfigFile.newBuilder.setName("testFile3").build))
  }

  test("Test EntityOwnerShip") {
    val publisher = new SilentEventPublishers

    val es = new EntityService()

    val s = new ConfigFileService(new ConfigFileServiceModelFactory(publisher))

    val node1 = one(es.put(Entity.newBuilder.setName("node1").addTypes("magic").build))
    val node2 = one(es.put(Entity.newBuilder.setName("node2").addTypes("magic").build))

    one(Status.CREATED, s.put(makeConfigFile("testFile1", "text", "blah", Some(node1))))
    one(Status.CREATED, s.put(makeConfigFile("testFile2", "text", "blah", Some(node1))))
    one(Status.CREATED, s.put(makeConfigFile("testFile3", "html", "blah", Some(node1))))
    one(Status.CREATED, s.put(makeConfigFile("testFile4", "html", "blah", Some(node2))))

    many(3, s.get(ConfigFile.newBuilder.setEntity(node1).build))
    many(1, s.get(ConfigFile.newBuilder.setEntity(node2).build))
  }

}
