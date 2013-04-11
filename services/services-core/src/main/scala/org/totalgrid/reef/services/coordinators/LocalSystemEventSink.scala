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
package org.totalgrid.reef.services.coordinators

import org.totalgrid.reef.event.SystemEventSink
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.exception.ReefServiceException

import org.totalgrid.reef.persistence.squeryl.DbConnection
import org.totalgrid.reef.services.framework.RequestContextSource
import org.totalgrid.reef.services.core.EventServiceModel
import net.agileautomata.executor4s.Executor

import org.totalgrid.reef.client.service.proto.Events.{ Event => EventProto }

class LocalSystemEventSink(executor: Executor) extends SystemEventSink with Logging {

  private var dbConnectionOption = Option.empty[DbConnection]
  private var components: Option[(RequestContextSource, EventServiceModel)] = None

  def publishSystemEvent(evt: EventProto) {
    executor.execute {
      publishSystemEventInAnotherThread(evt)
    }
  }

  private def publishSystemEventInAnotherThread(evt: EventProto) {
    try {
      // we need a different transaction so events are retained even if
      // we rollback the rest of the transaction because of an error
      dbConnectionOption.get.transaction {
        val (contextSource, model) = components.get
        contextSource.transaction { context =>
          // notice we are skipping the event service preCreate step that strips time and userId
          // because our local trusted service components have already set those values correctly
          model.createFromProto(context, evt)
        }
      }
    } catch {
      case e: ReefServiceException =>
        logger.warn("Service Exception thunking event: " + e.getMessage)
    }
  }

  def setComponents(dbConnection: DbConnection, model: EventServiceModel, contextSource: RequestContextSource) {
    dbConnectionOption = Some(dbConnection)
    components = Some(contextSource, model)
  }
}