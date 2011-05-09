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
package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.api.ISubscription
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.javaclient.IEventAcceptor
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.api.request.{ EventService }
import org.totalgrid.reef.api.request.builders.{ EventRequestBuilders, EventListRequestBuilders }
import org.totalgrid.reef.proto.Events.{ EventSelect, Event }
import org.totalgrid.reef.proto.Model.ReefUUID

trait EventServiceImpl extends ReefServiceBaseClass with EventService {

  def getEvent(uid: String) = {
    reThrowExpectationException("Event with UID: " + uid + " not found") {
      ops { _.getOneOrThrow(EventRequestBuilders.getByUID(uid)) }
    }
  }

  def getRecentEvents(limit: Int) = {
    val ret = ops { _.getOneOrThrow(EventListRequestBuilders.getAll(limit)) }
    ret.getEventsList
  }
  def getRecentEvents(limit: Int, sub: ISubscription[Event]) = {
    val ret = ops { _.getOneOrThrow(EventListRequestBuilders.getAll(limit), sub) }
    ret.getEventsList
  }
  def getRecentEvents(types: java.util.List[String], limit: Int) = {
    val ret = ops { _.getOneOrThrow(EventListRequestBuilders.getAllByEventTypes(types, limit)) }
    ret.getEventsList
  }
  def getEvents(selector: EventSelect) = {
    val ret = ops { _.getOneOrThrow(EventListRequestBuilders.getByEventSelect(selector)) }
    ret.getEventsList
  }
  def getEvents(selector: EventSelect, sub: ISubscription[Event]) = {
    val ret = ops { _.getOneOrThrow(EventListRequestBuilders.getByEventSelect(selector), sub) }
    ret.getEventsList
  }
  def publishEvent(event: Event) = {
    ops { _.putOneOrThrow(event) }
  }

  def createEventSubscription(callback: IEventAcceptor[Event]) = {
    ops { _.addSubscription(Descriptors.event.getKlass, callback.onEvent) }
  }

}