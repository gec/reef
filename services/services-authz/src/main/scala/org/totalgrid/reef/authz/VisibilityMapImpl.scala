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
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast._
import org.totalgrid.reef.models.{ SquerylConversions, ApplicationSchema }

class VisibilityMapImpl(permissions: List[Permission]) extends VisibilityMap {
  def selector(resourceId: String)(fun: (Query[UUID]) => LogicalBoolean) = {

    val entityQuery = constructQuery(permissions, resourceId, "read")

    entityQuery match {
      case DenyAll => (false === true)
      case AllowAll => (true === true)
      case Select(q) => fun(q)
    }
  }

  private sealed case class EntityQuery(allowAll: Option[Boolean], query: Option[Query[UUID]])
  private object DenyAll extends EntityQuery(Some(false), None)
  private object AllowAll extends EntityQuery(Some(true), None)
  private case class Select(q: Query[UUID]) extends EntityQuery(None, Some(q))

  private def constructQuery(permissions: List[Permission], service: String, action: String): EntityQuery = {
    // first filter down to permissions that have right service+action
    val applicablePermissions = permissions.filter(_.applicable(service, action))

    //println(service + ":" + action + " " + permissions + " -> " + applicablePermissions)

    if (applicablePermissions.isEmpty) {
      DenyAll
    } else {
      if (applicablePermissions.find(_.resourceDependent).isEmpty) {
        applicablePermissions.head.allow match {
          case true => AllowAll
          case false => DenyAll
        }
      } else {
        Select(from(ApplicationSchema.entities)(sql => where(makeSelector(applicablePermissions, sql.id)) select (sql.id)))
      }
    }
  }

  private def makeSelector(applicablePermissions: List[Permission], uuid: ExpressionNode): LogicalBoolean = {
    SquerylConversions.combineExpressions(applicablePermissions.map { perm =>
      val x: Option[LogicalBoolean] = (perm.selector(), perm.allow) match {
        case (Some(query), true) => Some(new BinaryOperatorNodeLogicalBoolean(uuid, new RightHandSideOfIn(query), "in", true))
        case (Some(query), false) => Some(new BinaryOperatorNodeLogicalBoolean(uuid, new RightHandSideOfIn(query), "not in", true))
        case _ => None
      }
      x
    }.flatten)
  }
}
