/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.persistence.squeryl

import java.sql.Connection
import org.squeryl.internals.DatabaseAdapter

import org.squeryl.Session

class SessionStats {
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
}

/**
 * wraps the squeryl Session so we can count actions and track which requests are making too many
 * queries to the database so its clear where we need to optimize.
 */
class CountingSession(c: Connection, d: DatabaseAdapter) extends Session {
  def connection = c
  def databaseAdapter = d

  val stats = new SessionStats
  def addQuery(s: String) {

    // TODO: replace if/else startsWith with match 
    if (s.startsWith("Select")) {
      stats.selects += 1
    } else if (s.startsWith("insert")) {
      stats.inserts += 1
    } else if (s.startsWith("update")) {
      stats.updates += 1
    } else if (s.startsWith("delete")) {
      stats.deletes += 1
    }
  }

  setLogger(addQuery)
}

object CountingSession {
  def currentSession = {
    Session.currentSession.asInstanceOf[CountingSession]
  }
}
