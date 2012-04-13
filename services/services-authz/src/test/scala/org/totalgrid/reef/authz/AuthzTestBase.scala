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
package org.totalgrid.reef.authz

import java.util.UUID
import org.totalgrid.reef.models._

class AuthzTestBase extends DatabaseUsingTestBase {

  case class TestEntity(name: String, types: List[String]) {
    val uuid = UUID.randomUUID()
  }

  def defineEntities(entities: List[TestEntity]) = {
    val es = entities.map { t =>
      val e = new Entity(t.name)
      e.id = t.uuid
      e
    }
    val types = entities.map { t => t.types.map { new EntityToTypeJoins(t.uuid, _) } }.flatten
    ApplicationSchema.entities.insert(es)
    ApplicationSchema.entityTypes.insert(types)

    entities.map { x => x.uuid }
  }

  def defineEdges(edges: List[EntityEdge]) {
    ApplicationSchema.edges.insert(edges)
  }
}
