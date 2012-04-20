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

import com.weiglewilczek.slf4s.Logging
import java.util.UUID
import org.squeryl.Query
import scala.Predef._
import java.awt.peer.ListPeer

object AuthzFilter extends AuthzFiltering(ResourceSpecificFilter)

/**
 * this is the first pass filtering, if we can determine that all of the applicable permissions are resource independent
 * we can very quickly and cheaply filter the payloads. If any of the permissions are resource specific we delegate
 * the filtering to the slower ResourceSpecificFiltering.
 */
class AuthzFiltering(resourceFilter: ResourceSpecificFiltering) extends AuthzFilteringService with Logging {

  def filter[A](permissions: => List[Permission], service: String, action: String, payloads: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]] = {

    // first filter down to permissions that have right service+action
    val applicablePermissions = permissions.filter(_.applicable(service, action))

    val results = if (applicablePermissions.isEmpty) {
      val defaultRule = unmatchedServiceAction(service, action)
      payloads.map { x => Denied[A](x, defaultRule) }
    } else {
      if (applicablePermissions.find(_.resourceDependent).isEmpty) {
        logger.info(service + ":" + action)
        val rule = applicablePermissions.head
        payloads.map { payload =>
          if (rule.allow) Allowed[A](payload, rule)
          else Denied[A](payload, rule)
        }
      } else {
        val uuidList = uuids // Call once

        logger.info(service + ":" + action + " -- " + EntityHelpers.getNames(uuidList.flatten.distinct).mkString("(", ",", ")"))
        resourceFilter.resourceSpecificFiltering(applicablePermissions, service, action, payloads.zip(uuidList))
      }
    }

    results
  }

  def selector(permissions: => List[Permission], service: String, action: String): Option[Query[UUID]] = {
    // first filter down to permissions that have right service+action
    val applicablePermissions = permissions.filter(_.applicable(service, action))

    if (applicablePermissions.isEmpty) {
      None
    } else {
      if (applicablePermissions.find(_.resourceDependent).isEmpty) {
        None
      } else {
        val l: List[Option[Query[UUID]]] = applicablePermissions.map { _.selector() }

        println(applicablePermissions)

        l.head

        //        l.find(_.isEmpty) match {
        //          case Some(empty) => None
        //          case None =>
        //            // TODO: merge queries
        //            l.head
        //        }
      }
    }
  }

  private def unmatchedServiceAction(service: String, action: String) = {
    Permission.denyAllPermission("No permission matched " + service + ":" + action + ". Assuming deny *")
  }

}
