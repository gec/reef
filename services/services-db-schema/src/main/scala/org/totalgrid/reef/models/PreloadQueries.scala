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
package org.totalgrid.reef.models

import java.util.UUID
import org.squeryl.PrimitiveTypeMode._

object PreloadQueries {
  def childrenToEntityMap[A <: EntityBasedModel](entityIds: List[UUID], relationship: String, requiredType: String) = {
    from(ApplicationSchema.edges, ApplicationSchema.entities, ApplicationSchema.entities, ApplicationSchema.entityTypes)((edge, parent, child, typ) =>
      where(
        (parent.id in entityIds) and
          (edge.parentId === parent.id) and
          (edge.childId === child.id) and
          (edge.relationship === relationship) and
          (typ.entityId === child.id) and
          (typ.entType === requiredType))
        select (parent.id, child)).toList.groupBy(_._1).mapValues { _.map { _._2 } }
  }

  def parentToEntityMap[A <: EntityBasedModel](entityIds: List[UUID], relationship: String, requiredType: String) = {
    from(ApplicationSchema.edges, ApplicationSchema.entities, ApplicationSchema.entities, ApplicationSchema.entityTypes)((edge, parent, child, typ) =>
      where(
        (child.id in entityIds) and
          (edge.parentId === parent.id) and
          (edge.childId === child.id) and
          (edge.relationship === relationship) and
          (typ.entityId === parent.id) and
          (typ.entType === requiredType))
        select (child.id, parent)).toList.map { t => t._1 -> t._2 }.toMap
  }
}
