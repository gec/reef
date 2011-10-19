package org.totalgrid.reef.api.sapi.client.rpc.impl

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import org.totalgrid.reef.api.japi.client.rpc.impl.builders.CommandRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.proto.Model.{ Command, Entity, Relationship }

@RunWith(classOf[JUnitRunner])
class CommandRequestTest
    extends ClientSessionSuite("Command.xml", "Command",
      <div>
        <p>
          A Command represents a configured output point. CommandAccess and UserCommandRequest services use
  this command name.
        </p>
        <p>
          Every Command is associated with an Entity of type "Command". The command's location in the
  system model is determined by this entity. Commands are also associated with entities designated
  as "logical nodes", which represent the communications interface/source.
        </p>
      </div>)
    with ShouldMatchers {

  test("Simple gets") {

    recorder.addExplanation("Get all", "Get all Commands")
    session.get(CommandRequestBuilders.getAll()).await.expectMany()

    /*val uidReq = Command.newBuilder.setUuid(allResp.head.getUuid).build
    val uidResp = client.getOrThrow(uidReq)

    doc.addCase("Get by UID", "Get", "Get point that matches a certain UID.", uidReq, uidResp)
    val nameReq = Command.newBuilder.setName(allResp.head.getName).build
    val nameResp = client.getOneOrThrow(nameReq)

    doc.addCase("Get by name", "Get", "Get point that matches a certain name.", nameReq, nameResp)
                                                                                      */
  }

  test("Entity query") {

    val desc = <div>
                 Given an Entity of type "Command", the service can return the corresponding Command object.
               </div>

    recorder.addExplanation("Get by entity", desc)
    session.get(CommandRequestBuilders.getByEntityName("StaticSubstation.Breaker02.Trip")).await.expectMany()
  }

  /*test("Entity tree query") {
    val entDesc = Entity.newBuilder.setName("StaticSubstation.Breaker02").addRelations(
      Relationship.newBuilder.setRelationship("owns").setDescendantOf(true).addEntities(
        Entity.newBuilder.addTypes("Command"))).build

    val req = Command.newBuilder.setEntity(entDesc).build
    val resp = client.getOrThrow(req)

    val desc = <div>
                 Search for commands using an entity tree query. The entity field can be any entity query; any entities of
      type "Command" that are found will have their corresponding Command objects added to the result set.
               </div>

    doc.addCase("Get by entity query", "Get", desc, req, resp)
  }*/

}