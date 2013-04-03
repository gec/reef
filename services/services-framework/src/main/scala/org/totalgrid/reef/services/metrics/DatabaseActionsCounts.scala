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
package org.totalgrid.reef.services.metrics

class DatabaseActionsCounts {
  var selects = 0
  var updates = 0
  var inserts = 0
  var deletes = 0

  /**
   * total number of actions in the session.
   */
  def actions = selects + updates + inserts + deletes

  override def toString = {
    "Total: " + actions + " S: " + selects + " U: " + updates + " I: " + inserts + " D: " + deletes
  }

  def addQuery(s: String) {

    // TODO: replace if/else startsWith with match
    if (s.startsWith("Select")) {
      selects += 1
    } else if (s.startsWith("insert")) {
      inserts += 1
    } else if (s.startsWith("update")) {
      updates += 1
    } else if (s.startsWith("delete")) {
      deletes += 1
    }
  }
}