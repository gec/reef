/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.persistence.squeryl

import org.squeryl.PrimitiveTypeMode
import org.squeryl.Session

/**
 * contains the session factory necessary to talk to the database. Handles the setting up of the
 * session and transaction blocks.
 *
 * These functions should be used instead of PrimitiveTypeMode.transaction
 */
trait DbConnection {

  /**
   * will open and close a new transaction around the passed in code
   */
  def transaction[A](fun: => A): A

  /**
   * will open a new transaction if one doesn't already exist, otherwise goes into same transaction
   */
  def inTransaction[A](fun: => A): A
}

/**
 * default DbConnection implementation that takes a session factory function and generates
 * a new session whenever a new transaction is needed
 */
class SessionDbConnection(sessionFactory: () => Session) extends DbConnection {

  def transaction[A](fun: => A): A = {
    val session = sessionFactory()
    val result = PrimitiveTypeMode.using(session) {
      PrimitiveTypeMode.transaction(session) {
        fun
      }
    }
    result
  }

  def inTransaction[A](fun: => A): A = {

    if (Session.hasCurrentSession) fun
    else transaction(fun)

  }
}