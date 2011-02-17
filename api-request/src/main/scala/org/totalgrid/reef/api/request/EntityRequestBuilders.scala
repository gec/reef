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

import org.totalgrid.reef.proto.Model.{ Entity, Relationship }

object EntityRequestBuilders {

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