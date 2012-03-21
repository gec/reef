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


trait ResourceSpecificFiltering {
  /**
   * each payload object is associated with a list of related resources, if any of those resources are restricted we will
   * return a denied object for each entry
   */
  def resourceSpecificFiltering[A](applicablePermissions: List[Permission], pairs: List[(A, List[UUID])]): List[FilteredResult[A]]
}


object ResourceSpecificFilter extends ResourceSpecificFiltering {

  def resourceSpecificFiltering[A](applicablePermissions: List[Permission], pairs: List[(A, List[UUID])]): List[FilteredResult[A]] = {

    val originalStates = pairs.map { case (payload, uuids) => SelectState[A](payload, uuids, None) }

    val finalStates = applicablePermissions.foldLeft(originalStates) {
      case (states, permission) =>
        permission.checkMatches(states)
    }

    // we will only make the defaultRule with helpful (and expensive) string if something didn't match any permission
    lazy val unmatched = finalStates.filter { _.filteredResult.isEmpty }
    lazy val defaultRule = unmatchedResources(unmatched.map { _.uuids }.flatten)

    finalStates.map { _.filteredResult.getOrElse(Denied[A](defaultRule)) }
  }

  private def unmatchedResources(unmatchedUuids: List[UUID]) = {

    lazy val unmatchNames = EntityHelpers.getNames(unmatchedUuids)
    lazy val msg = "No permission selector matched " + unmatchNames.mkString("(", ",", ")") + ". Assuming deny *."

    Permission.denyAllPermission(msg)
  }
}
