/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ Entity, Relationship }

@RunWith(classOf[JUnitRunner])
class EntityRequestTest
    extends ServiceClientSuite("Entity.xml", "Entity",
      <div>
        <p>
          An Entity represents a generic component of a system model. Entities are modeled by a name
        and some number of types which describe the entity's role in the system. Entities
        also contain a list of Relationship objects which describe connections/associations
        with other entities in the system.
        </p>
        Entity relationships have a type ("relationship"), a direction ("descendant_of"), and
  a list of Entity objects the relationship connects to. Because relationships are transitive,
  they also have a distance, or how many edges away the other entities are.
        <p>
          Together, entities connected by a certain relationship type form an acyclic directed
        graph. The model must not include cycles (given a start entity, it must not be possible
        to get back to the same entity using the same relationship type and direction).
        </p>
      </div>)
    with ShouldMatchers {

  test("Simple gets") {
    val req = EntityRequestBuilders.all
    val resp = client.getOrThrow(req)

    doc.addCase("Get all entities", "Get", "Return all entities in the system. (Shows first three responses)", req, resp.take(3))

    val targetUid = resp.head.getUid
    val singleReq = EntityRequestBuilders.forId(targetUid)
    val singleResp = client.getOneOrThrow(singleReq)

    doc.addCase("Get by UID", "Get", "Finds a specific entity by UID.", singleReq, singleResp)

    val typReq = EntityRequestBuilders.forType("Breaker")
    val typResp = client.getOrThrow(typReq)

    doc.addCase("Get by type", "Get", "Find all entities that match a given type.", typReq, typResp)

  }

  test("Children") {
    val subs = client.getOrThrow(EntityRequestBuilders.forType("Substation"))
    val subUid = subs.head.getUid

    val req = EntityRequestBuilders.selectChildren(subUid, None, Nil, true)
    val resp = client.getOrThrow(req)

    doc.addCase("Get descendants", "Get", "Finds all descendants of the root entity with the relationship \"owns\".", req, resp)

    val directReq = EntityRequestBuilders.selectChildren(subUid, None, Nil, false)
    val directResp = client.getOrThrow(directReq)

    doc.addCase("Get direct descendants", "Get", "Finds all descendants of the root entity with the relationship \"owns\" and that are a distance 1 away.", directReq, directResp)

    val descTypReq = EntityRequestBuilders.childrenOfType(subUid, "Point")
    val descTypResp = client.getOrThrow(descTypReq)

    doc.addCase("Get descendants of a type", "Get", "Finds all descendants of the root entity with the relationship \"owns\" and that are of type \"Point\".", descTypReq, descTypResp)
  }

  test("Abstract root") {
    val req = Entity.newBuilder.addTypes("Point").addRelations(
      Relationship.newBuilder.setDescendantOf(true).setRelationship("feedback").addEntities(
        Entity.newBuilder.addTypes("Command"))).build
    val resp = client.getOrThrow(req)

    val desc = <div>Instead of starting the query with a UID, this finds all the entities of type "Point" and any "feedback" relationships to entities of type "Command"</div>

    doc.addCase("Abstract root set", "Get", desc, req, resp)
  }

  test("Multilevel") {
    val subs = client.getOrThrow(EntityRequestBuilders.forType("Substation"))
    val subUid = subs.head.getUid

    val req = Entity.newBuilder.setUid(subUid).addRelations(
      Relationship.newBuilder.setDescendantOf(true).setRelationship("owns").addEntities(
        Entity.newBuilder.addTypes("Breaker").addRelations(
          Relationship.newBuilder.setDescendantOf(true).setRelationship("owns").addEntities(
            Entity.newBuilder.addTypes("Point"))))).build

    val resp = client.getOrThrow(req)

    val desc = <div>Starting from a root node ("Substation"), the request asks for children of type "Breaker", and children of those of type "Point".</div>

    doc.addCase("Multi-level tree query", "Get", desc, req, resp)
  }
}