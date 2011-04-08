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
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Alarms.Alarm
import org.totalgrid.reef.api.{ ISubscription, ReefServiceException }
import org.totalgrid.reef.api.javaclient.IEventAcceptor

/**
 *
 * Alarms are a refinement of events which identify system occurrences that require operator intervention.
 * Alarm objects are tied closely to event objects. All alarms are associated with events, but not all events cause alarms.
 *
 * In contrast to events, alarms have persistent state. The three principal alarm states are unacknowledged,
 * acknowledged, and removed. The transitions between these states constitute the alarm lifecycle, and
 * manipulation of the states involves user workflow.
 *
 * Transitions in alarm state may themselves be events, as they are part of the record of user operations.
 *
 * During the configuration process, the system designer decides what events trigger alarms. The primary consumers of
 * alarms are operators tasked with monitoring the system in real-time and responding to abnormal conditions.
 */
trait AlarmService {

  /**
   * get a single alarm
   * @param uuid UUID of event
   */
  @throws(classOf[ReefServiceException])
  def getAlarm(uuid: ReefUUID): Alarm

  /**
   * get the most recent alarms
   * @param limit the number of incoming alarms
   */
  @throws(classOf[ReefServiceException])
  def getActiveAlarms(limit: Int): java.util.List[Alarm]

  /**
   * get the most recent alarms and setup a subscription to all future alarms
   * @param limit the number of incoming events
   * @param sub a subscription object that consumes the new Alarms coming in
   */
  @throws(classOf[ReefServiceException])
  def getActiveAlarms(limit: Int, sub: ISubscription[Alarm]): java.util.List[Alarm]

  /**
   * get the most recent alarms
   * @param types event type names
   * @param limit the number of incoming alarms
   */
  @throws(classOf[ReefServiceException])
  def getActiveAlarms(types: java.util.List[String], limit: Int): java.util.List[Alarm]

  /**
   * silences an audible alarm
   */
  @throws(classOf[ReefServiceException])
  def silenceAlarm(alarm: Alarm): Alarm

  /**
   * acknowledge the alarm (silences if not already silenced)
   */
  @throws(classOf[ReefServiceException])
  def acknowledgeAlarm(alarm: Alarm): Alarm

  /**
   * "remove" an Alarm from the active list.
   */
  @throws(classOf[ReefServiceException])
  def removeAlarm(alarm: Alarm): Alarm

  /**
   * Create a subscription object that can receive Alarms.
   * @return "blank" subscription object, needs to have the subscription configured by passing it with another request
   */
  def createAlarmSubscription(callback: IEventAcceptor[Alarm]): ISubscription[Alarm]
}