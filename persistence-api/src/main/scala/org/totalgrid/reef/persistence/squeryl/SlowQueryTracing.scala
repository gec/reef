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

import org.squeryl.internals.{ StatementWriter, DatabaseAdapter }
import org.squeryl.Session
import java.sql.SQLException

import org.totalgrid.reef.util.Timing
import org.totalgrid.reef.util.Logging

trait SlowQueryTracing extends DatabaseAdapter with Logging {

  val slowQueryTimeMilli: Long

  override def execFailSafeExecute(sw: StatementWriter, silenceException: SQLException => Boolean): Unit = {
    val timingFun = monitorSlowQueries(slowQueryTimeMilli, sw) _
    Timing.time(timingFun) {
      super.execFailSafeExecute(sw, silenceException)
    }
  }

  override def exec[A](s: Session, sw: StatementWriter)(block: => A): A = {
    val timingFun = monitorSlowQueries(slowQueryTimeMilli, sw) _
    Timing.time[A](timingFun) {
      super.exec[A](s, sw)(block)
    }
  }

  def monitorSlowQueries(maxTimeMilli: Long, sw: StatementWriter)(actualTimeMilli: Long): Unit = {
    if (actualTimeMilli >= maxTimeMilli) {
      logger.info("SlowQuery, actual: " + actualTimeMilli + "ms max allowed: " + maxTimeMilli + "ms, query: " + sw.toString)
    }
  }
}