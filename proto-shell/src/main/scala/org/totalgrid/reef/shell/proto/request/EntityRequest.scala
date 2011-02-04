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
package org.totalgrid.reef.shell.proto.request

import org.totalgrid.reef.proto.Model.{ Relationship, Entity }
import org.totalgrid.reef.protoapi.client.SyncOperations
import RequestFailure._
import org.totalgrid.reef.proto.Processing.TriggerSet

object EntityRequest {

  def getAll(client: SyncOperations) = {
    val results = client.get(EntityRequest.all)
    if (results.isEmpty) throw RequestFailure("No entities found.")
    results
  }
  def getById(id: String, client: SyncOperations) = {
    interpretAs("Entity not found.") {
      client.getOne(EntityRequest.forId(id))
    }
  }
  def getAllOfType(typ: String, client: SyncOperations) = {
    client.get(forType(typ))
  }

  def getChildren(parentId: String, relType: Option[String], subTypes: List[String], anyDepth: Boolean, client: SyncOperations) = {
    val ents = client.get(EntityRequest.selectChildren(parentId, relType, subTypes, anyDepth))
    if (ents.isEmpty) throw RequestFailure("Root entity not found.")
    ents
  }

  def getAllTriggers(client: SyncOperations) = {
    val sets = interpretAs("Triggers not found.") {
      client.get(TriggerSet.newBuilder.build).toList
    }
    if (sets.isEmpty) throw RequestFailure("No triggers found.")

    sets
  }

  def getTriggers(pointId: String, client: SyncOperations) = {
    val point = interpretAs("Point not found.") {
      client.getOne(PointRequest.forEntityRequest(builderForId(pointId).build))
    }
    interpretAs("Trigger set not found.") {
      client.getOne(TriggerSet.newBuilder.setPoint(point).build)
    }
  }

  def all = Entity.newBuilder.setUid("*").build

  def forId(id: String) = {
    builderForId(id).build
  }

  def forType(typ: String) = forTypes(List(typ))
  def forTypes(typs: List[String]) = {
    val req = Entity.newBuilder
    typs.foreach(typ => req.addTypes(typ))
    req.build
  }

  def childrenOfType(parentId: String, typ: String) = {
    val req = builderForId(parentId)

    val rel = Relationship.newBuilder
      .setDescendantOf(true)
      .setRelationship("owns")
      .addEntities(Entity.newBuilder.addTypes(typ))

    req.addRelations(rel).build
  }

  def selectChildren(parentId: String, relType: Option[String], subTypes: List[String], anyDepth: Boolean) = {
    val req = builderForId(parentId)

    val rel = Relationship.newBuilder
      .setDescendantOf(true)
      .setRelationship(relType getOrElse ("owns"))

    if (!anyDepth) rel.setDistance(1)

    subTypes.foreach(typ => rel.addEntities(Entity.newBuilder.addTypes(typ)))

    req.addRelations(rel).build
  }

  protected def builderForId(id: String) = {
    val numOpt = try {
      Some(Integer.parseInt(id.trim))
    } catch {
      case ex: NumberFormatException => None
    }
    numOpt match {
      case Some(num) => Entity.newBuilder.setUid(num.toString)
      case None => Entity.newBuilder.setName(id)
    }
  }
}