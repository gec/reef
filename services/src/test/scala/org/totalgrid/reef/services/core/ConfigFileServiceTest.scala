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

import org.totalgrid.reef.japi.Envelope.Status

import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.proto.Model.{ ReefUUID, ConfigFile, Entity }

import org.totalgrid.reef.services.core.SyncServiceShims._
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.japi.{ BadRequestException, Envelope }

@RunWith(classOf[JUnitRunner])
class ConfigFileServiceTest extends DatabaseUsingTestBase {

  def makeConfigFile(name: String, mime: String, data: String, owner: Option[Entity] = None) = {
    import org.totalgrid.reef.messaging.ProtoSerializer.convertStringToByteString
    val cfb = ConfigFile.newBuilder.setName(name).setMimeType(mime).setFile(data)
    owner.foreach(cfb.addEntities(_))
    cfb.build
  }

  def makeConfigFile(mime: String, data: String, owner: Option[Entity]) = {
    import org.totalgrid.reef.messaging.ProtoSerializer.convertStringToByteString
    val cfb = ConfigFile.newBuilder.setMimeType(mime).setFile(data)
    owner.foreach(cfb.addEntities(_))
    cfb.build
  }

  test("Test Status Codes") {
    val s = new ConfigFileService(new ConfigFileServiceModel)

    val configFile = makeConfigFile("testFile1", "text", "blah")

    val result = s.put(configFile).expectOne(Status.CREATED)

    s.put(configFile).expectOne(Status.NOT_MODIFIED).getUuid should equal(result.getUuid)

    val configFileMod = makeConfigFile("testFile1", "text", "im different!")
    s.put(configFileMod).expectOne(Status.UPDATED).getUuid should equal(result.getUuid)

    val configFileMod2 = makeConfigFile("testFile1", "mimediff", "im different!")
    val finalObject = s.put(configFileMod2).expectOne(Status.UPDATED)

    s.delete(finalObject).expectOne(Status.DELETED).getUuid should equal(result.getUuid)
  }

  test("Test Searching") {
    val s = new ConfigFileService(new ConfigFileServiceModel)

    val configFile1 = makeConfigFile("testFile1", "text", "blah")
    val configFile2 = makeConfigFile("testFile2", "text", "blah")
    val configFile3 = makeConfigFile("testFile3", "html", "blah")
    val configFile4 = makeConfigFile("testFile4", "html", "blah")

    s.put(configFile1).expectOne(Status.CREATED)
    s.put(configFile2).expectOne(Status.CREATED)
    s.put(configFile3).expectOne(Status.CREATED)
    s.put(configFile4).expectOne(Status.CREATED)

    s.get(ConfigFile.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build).expectMany(4)
    s.get(ConfigFile.newBuilder.setMimeType("text").build).expectMany(2)
    s.get(ConfigFile.newBuilder.setMimeType("html").build).expectMany(2)
    s.get(ConfigFile.newBuilder.setMimeType("xml").build).expectNone()

    s.get(ConfigFile.newBuilder.setName("testFile3").build).expectOne()
  }

  test("Test EntityOwnerShip") {
    val es = new EntityService()

    val s = new ConfigFileService(new ConfigFileServiceModel)

    val node1 = es.put(Entity.newBuilder.setName("node1").addTypes("magic").build).expectOne()
    val node2 = es.put(Entity.newBuilder.setName("node2").addTypes("magic").build).expectOne()

    s.put(makeConfigFile("testFile1", "text", "blah", Some(node1))).expectOne(Status.CREATED)
    s.put(makeConfigFile("testFile2", "text", "blah", Some(node1))).expectOne(Status.CREATED)
    s.put(makeConfigFile("testFile3", "html", "blah", Some(node1))).expectOne(Status.CREATED)
    s.put(makeConfigFile("testFile4", "html", "blah", Some(node2))).expectOne(Status.CREATED)

    s.get(ConfigFile.newBuilder.addEntities(node1).build).expectMany(3)
    s.get(ConfigFile.newBuilder.addEntities(node2).build).expectOne()
  }

  test("Test add config file without name") {
    val es = new EntityService()

    val s = new ConfigFileService(new ConfigFileServiceModel)

    val node1 = es.put(Entity.newBuilder.setName("node1").addTypes("magic").build).expectOne()

    val cf = s.put(makeConfigFile("text", "blah", Some(node1))).expectOne(Status.CREATED)

    cf.getName should equal(cf.getUuid.getUuid)

    s.put(makeConfigFile("text", "blah", Some(node1))).expectOne(Status.CREATED)

    s.get(ConfigFile.newBuilder.addEntities(node1).build).expectMany(2)
  }

  test("Shared Entity Ownership") {
    val es = new EntityService()

    val s = new ConfigFileService(new ConfigFileServiceModel)

    val node1 = es.put(Entity.newBuilder.setName("node1").addTypes("magic").build).expectOne()
    val node2 = es.put(Entity.newBuilder.setName("node2").addTypes("magic").build).expectOne()
    val node3 = es.put(Entity.newBuilder.setName("node3").addTypes("magic").build).expectOne()

    s.put(makeConfigFile("sharedFile", "text", "blah", Some(node1))).expectOne(Status.CREATED)
    s.put(makeConfigFile("sharedFile", "text", "blah", Some(node2))).expectOne(Status.UPDATED)
    s.put(makeConfigFile("testFile1", "html", "blah", Some(node1))).expectOne(Status.CREATED)
    s.put(makeConfigFile("testFile2", "html", "blah", Some(node2))).expectOne(Status.CREATED)

    s.get(ConfigFile.newBuilder.addEntities(node1).build).expectMany(2)
    s.get(ConfigFile.newBuilder.addEntities(node2).build).expectMany(2)

    // if we ask for multiple entities we get the union of their config files
    s.get(ConfigFile.newBuilder.addEntities(node1).addEntities(node2).build).expectMany(3)

    s.get(ConfigFile.newBuilder.addEntities(node3).build).expectNone()
  }

  test("Test ResultLimit") {
    val s = new ConfigFileService(new ConfigFileServiceModel)

    (1 to 50).foreach { i =>
      val configFile = makeConfigFile("testFile" + i, "text", "blah")
      s.put(configFile).expectOne(Status.CREATED)
    }

    val query = ConfigFile.newBuilder.setName("*").build

    val env = BasicRequestHeaders.empty

    s.get(query, env.setResultLimit(1000)).expectMany(50)

    s.get(query, env.setResultLimit(0)).expectMany(0)

    s.get(query, env.setResultLimit(1)).expectMany(1)

    s.get(query, env.setResultLimit(45)).expectMany(45)

    intercept[BadRequestException] {
      s.get(query, env.setResultLimit(-1))
    }
  }

}
