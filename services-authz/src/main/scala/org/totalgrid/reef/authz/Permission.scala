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
import org.totalgrid.reef.client.service.proto.Auth.PermissionSet

import scala.collection.JavaConversions._

object Permission {
  def fromProto(permissionSet: PermissionSet, agentName: String): List[Permission] = {

    permissionSet.getPermissionsList.toList.map { perm =>

      def nonRedundant(list: List[String]) = if (list.contains("*")) List("*") else list

      val actions = nonRedundant(perm.getVerbList.toList)
      val resources = nonRedundant(perm.getResourceList.toList)
      val selectorStrings = if (perm.getSelectorCount == 0) List("*") else nonRedundant(perm.getSelectorList.toList.map { _.getName })

      //val selectors = selectorStrings.map{  }

      new Permission(perm.getAllow, resources, actions, ResourceSelectorFactory.build(selectorStrings.head, agentName))
    }
  }

  def denyAllPermission(msg: => String) = {
    new Permission(false, "*", "*") {

      override def toString() = msg
    }
  }
}

case class SelectState[A](payload: A, uuids: List[UUID], filteredResult: Option[FilteredResult[A]])

class Permission(val allow: Boolean, services: List[String], actions: List[String], matcher: ResourceSelector) {

  def this(allow: Boolean, service: String, action: String) = this(allow, List(service), List(action), new WildcardMatcher)

  def applicable(s: String, a: String) = (services == List("*") || services.find(_ == s).isDefined) && (actions == List("*") || actions.find(_ == a).isDefined)

  def resourceDependent = matcher.resourceDependent

  def checkMatches[A](toBeMatched: List[SelectState[A]]): List[SelectState[A]] = {

    toBeMatched.map {
      case state: SelectState[A] =>
        if (state.filteredResult.isDefined) state
        else {
          val matched = matcher.includes(state.uuids)
          val result = matched.find(_ == Some(true)) match {
            case Some(Some(true)) =>
              allow match {
                case true => Some(Allowed[A](state.payload, this))
                case false => Some(Denied[A](this))
              }
            case _ =>
              None
          }
          state.copy(filteredResult = result)
        }
    }
  }
}