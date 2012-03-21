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

import org.totalgrid.reef.client.exception.UnauthorizedException
import org.totalgrid.reef.client.service.proto.Auth.EntitySelector

import scala.collection.JavaConversions._

object ResourceSelectorFactory {
  def build(selectors: List[EntitySelector], agentName: String): ResourceSelector = {

    if (selectors.isEmpty) new WildcardMatcher
    else {
      if (selectors.size > 1) throw new UnauthorizedException("Only one selector at a time is implemented.")
      val selector = selectors.head
      selector.getStyle match {
        case "*" => new WildcardMatcher
        case "self" => new EntityHasName(List(agentName))
        case "type" => new EntityTypeIncludes(selector.getArgumentsList.toList)
        case _ =>
          throw new UnauthorizedException("Unknown selector style: " + selector.getStyle + ". Valid styles are (*, self, type, parent, child)")
      }
    }
  }
}
