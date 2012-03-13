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

import org.totalgrid.reef.client.exception.{ UnauthorizedException }

object ListOps {
  def first[T, A](list: List[T], p1: T => Option[A]): Option[(T, A)] = {
    list.iterator.map(x => (x, p1(x)))
      .find(_._2.isDefined).map { x => (x._1, x._2.get) }
  }
}
import ListOps._

object AuthzFilteringService {

  //  def filterByApplication[A](service : String, action : String, applications : List[(ApplicationInstance,A)]) : List[FilteredResult[A]]
  //
  //  def filterByAgent[A](service : String, action : String, agents: List[(Agent,A)]) : List[FilteredResult[A]]

  def filter[A](permissions: => List[Permission], service: String, action: String, payloads: List[A], entities: => List[AuthEntity]): List[FilteredResult[A]] = {

    // figure out which permissions may apply to this request
    val applicablePermissions = permissions.filter(_.applicable(service, action))

    if (applicablePermissions.isEmpty) {
      throw new UnauthorizedException("No permission matched " + service + ":" + action + ". Assuming deny *")
    } else {
      if (applicablePermissions.find(_.resourceDependent).isEmpty) {
        val rule = applicablePermissions.head
        payloads.map { payload =>
          if (rule.allow) Allowed[A](payload, rule)
          else Denied[A](rule)
        }
      } else {
        filterByEntity(applicablePermissions, entities.zip(payloads))
      }
    }
  }

  def filterByEntity[A](applicablePermissions: List[Permission], entities: List[(AuthEntity, A)]): List[FilteredResult[A]] = {
    entities.map {
      case (entity, payload) =>
        val firstApplicable: Option[(Permission, MatcherResult)] = first(applicablePermissions, { x: Permission => x.includes(entity) })
        firstApplicable match {
          case Some((rule, matcher)) => if (rule.allow && matcher.allow) Allowed[A](payload, rule) else Denied[A](rule)
          case None => throw new UnauthorizedException("No permission selector matched " + entity.name + ". Assuming deny *.")
        }
    }
  }
}

trait AuthEntity {
  def name: String
  def types: List[String]
}

case class Permission(allow: Boolean, service: String, action: String, resourceSets: List[ResourceSet]) {
  def applicable(s: String, a: String) = (service == "*" || service == s) && (action == "*" || action == a)

  def resourceDependent = resourceSets.find(_.resourceDependent).isDefined

  def includes(e: AuthEntity): Option[MatcherResult] =
    first(resourceSets, { x: ResourceSet => x.includes(e) }).map { x => x._2 }
}

case class ResourceSet(matchers: List[Matcher]) {
  def includes(e: AuthEntity): Option[MatcherResult] =
    first(matchers, { x: Matcher => x.includes(e) }).map { x => MatcherResult(x._1, x._2) }

  def resourceDependent = matchers.find(_.resourceDependent).isDefined
}

sealed trait Matcher {
  def includes(e: AuthEntity): Option[Boolean]

  def allow: Boolean

  def resourceDependent: Boolean
}

class EntityTypeIncludesMatcher(types: List[String]) extends Matcher {
  def includes(e: AuthEntity) = e.types.find(typ => types.find(typ == _).isDefined).map { s => true }
  val allow = true
  val resourceDependent = true

  override def toString() = "entity.types include " + types.mkString("(", ",", ")")
}

class EntityTypeDoesntIncludeMatcher(types: List[String]) extends Matcher {
  def includes(e: AuthEntity) = e.types.find(typ => types.find(typ == _).isDefined).map { s => false }
  val allow = false
  val resourceDependent = true

  override def toString() = "entity.types doesnt include " + types.mkString("(", ",", ")")
}

class EntityHasName(name: String) extends Matcher {
  def includes(e: AuthEntity) = Some(name == e.name)
  val allow = true
  val resourceDependent = true

  override def toString() = "entity.name is not " + name
}

class AllMatcher extends Matcher {
  def includes(e: AuthEntity) = Some(true)
  val allow = true
  val resourceDependent = false
  override def toString() = "*"
}

case class MatcherResult(matcher: Matcher, allow: Boolean)

trait FilteredResult[A] {
  def result: Option[A]
  def isAllowed: Boolean
  def permission: Permission
}

case class Allowed[A](a: A, permission: Permission) extends FilteredResult[A] {
  def isAllowed = true
  def result = Some(a)
}
case class Denied[A](permission: Permission) extends FilteredResult[A] {
  def isAllowed = false
  def result = Option.empty[A]
}
