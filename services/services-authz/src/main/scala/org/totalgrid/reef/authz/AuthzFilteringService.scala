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

sealed trait FilteredResult[A] {
  def result: A
  def isAllowed: Boolean
  def permission: Permission
}

case class Allowed[A](a: A, permission: Permission) extends FilteredResult[A] {
  def isAllowed = true
  def result = a
}
case class Denied[A](a: A, permission: Permission) extends FilteredResult[A] {
  def isAllowed = false
  def result = a
}

trait AuthzFilteringService {
  def filter[A](permissions: => List[Permission], service: String, action: String, payloads: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]]

  def selector(permissions: => List[Permission], service: String, action: String): Option[Query[UUID]]
}
