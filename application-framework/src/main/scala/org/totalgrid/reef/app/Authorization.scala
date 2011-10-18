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
package org.totalgrid.reef.app

import org.totalgrid.reef.api.proto.Auth._
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.sapi.client.SessionPool
import org.totalgrid.reef.japi.ReefServiceException
import org.totalgrid.reef.util.Logging
import net.agileautomata.executor4s.{ Failure, Success }

object Authorization extends Logging {

  def defaultUserName = SystemProperty.get("reef.user", "system")
  def defaultUserPassword = SystemProperty.get("reef.user.password", "system")

  def buildLogin(userName: Option[String] = None, userPassword: Option[String] = None): AuthToken = {
    val agent = Agent.newBuilder
    agent.setName(userName.getOrElse(defaultUserName)).setPassword(userPassword.getOrElse(defaultUserPassword))
    val auth = AuthToken.newBuilder
    auth.setAgent(agent)
    auth.build
  }

  def login(pool: SessionPool, executor: Executor, retryMs: Int)(callback: (AuthToken) => Unit): Unit = {

    def retry = executor.delay(retryMs) {
      login(pool, executor, retryMs)(callback)
    }

    def initiate = pool.borrow {
      _.put(buildLogin()).listen {
        _.one match {
          case Success(auth) => callback(auth)
          case Failure(ex) =>
            logger.error("Error getting auth token: " + ex.getMessage)
            retry
        }
      }
    }

    try {
      initiate
    } catch {
      case ex: ReefServiceException =>
        logger.error("Exception while getting auth token: " + ex)
        retry
    }

  }

}