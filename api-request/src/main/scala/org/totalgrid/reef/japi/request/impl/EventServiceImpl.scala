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
package org.totalgrid.reef.japi.request.impl

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.japi.request.{ EventService }
import org.totalgrid.reef.japi.request.builders.{ EventRequestBuilders, EventListRequestBuilders }
import org.totalgrid.reef.proto.Events.{ EventSelect, Event }

import scala.collection.JavaConversions._

trait EventServiceImpl extends ReefServiceBaseClass with EventService {

  override def getEvent(uid: String) = ops("Couldn't get event with uid: " + uid) {
    _.get(EventRequestBuilders.getByUID(uid)).await().expectOne
  }

  override def getRecentEvents(limit: Int) = ops("Couldn't get the last " + limit + " recent events") {
    _.get(EventListRequestBuilders.getAll(limit)).await().expectOne.getEventsList
  }

  override def subscribeToRecentEvents(limit: Int) = {
    ops("Couldn't subscribe to recent events") { session =>
      useSubscription(session, Descriptors.event.getKlass) { sub =>
        session.get(EventListRequestBuilders.getAll(limit), sub).await().expectOne.getEventsList
      }
    }
  }

  override def subscribeToRecentEvents(types: java.util.List[String], limit: Int) = {
    ops("Couldn't subscribe to recent events") { session =>
      useSubscription(session, Descriptors.event.getKlass) { sub =>
        session.get(EventListRequestBuilders.getAllByEventTypes(types, limit), sub).await().expectOne.getEventsList
      }
    }
  }

  override def getRecentEvents(types: java.util.List[String], limit: Int) = {
    ops("Couldn't get recent events with types: " + types) {
      _.get(EventListRequestBuilders.getAllByEventTypes(types, limit)).await().expectOne.getEventsList
    }
  }

  override def getEvents(selector: EventSelect) = ops("Couldn't get events matching: " + selector) {
    _.get(EventListRequestBuilders.getByEventSelect(selector)).await().expectOne.getEventsList
  }

  override def subscribeToEvents(selector: EventSelect) = {
    ops("Couldn't subscribe to events matching: " + selector) { session =>
      useSubscription(session, Descriptors.event.getKlass) { sub =>
        session.get(EventListRequestBuilders.getByEventSelect(selector), sub).await().expectOne.getEventsList
      }
    }
  }

}