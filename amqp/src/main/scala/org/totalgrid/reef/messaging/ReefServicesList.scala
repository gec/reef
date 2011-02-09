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
package org.totalgrid.reef.messaging

import javabridge.{ ProtoDescriptor, Deserializers }

object ReefServiceMap {
  val servicemap: ServiceList.ServiceMap = Map(

    getEntry(Deserializers.port, "front_end_port"),
    getEntry(Deserializers.frontEndProcessor, "front_end_processor"),
    getEntry(Deserializers.communicationEndpointConfig, "comm_endpoint"),
    getEntry(Deserializers.communicationEndpointConnection, "front_end_assignment"),
    getEntry(Deserializers.measurementProcessingConnection, "meas_proc_assignment"),

    getEntry(Deserializers.measurementBatch, "measurement_batch"),
    getEntry(Deserializers.measurementHistory, "measurement_history"),
    getEntry(Deserializers.measurementSnapshot, "measurement_snapshot", Some(Deserializers.measurement), Some("measurement")),
    getEntry(Deserializers.measOverride, "meas_override"),
    getEntry(Deserializers.triggerSet, "trigger_set"),
    getEntry(Deserializers.statusSnapshot, "process_status"),

    getEntry(Deserializers.event, "event"),
    getEntry(Deserializers.eventList, "event_list"),
    getEntry(Deserializers.eventConfig, "event_config"),
    getEntry(Deserializers.alarm, "alarm"),
    getEntry(Deserializers.alarmList, "alarm_list"),
    getEntry(Deserializers.authToken, "auth_token"),

    getEntry(Deserializers.userCommandRequest, "user_command_request"),
    getEntry(Deserializers.commandAccess, "command_access"),

    getEntry(Deserializers.applicationConfig, "app_config"),

    getEntry(Deserializers.configFile, "config_file"),
    getEntry(Deserializers.command, "command"),
    getEntry(Deserializers.point, "point"),
    getEntry(Deserializers.entity, "entity"),
    getEntry(Deserializers.entityEdge, "entity_edge"))

  private def getEntry[A, B](descriptor: ProtoDescriptor[A], exchange: String, subClass: Option[ProtoDescriptor[B]] = None, subExchange: Option[String] = None): ServiceList.ServiceTuple = {
    (descriptor.getKlass -> ServiceInfo(
      exchange,
      descriptor,
      subClass.isDefined,
      subClass.getOrElse(descriptor),
      subExchange.getOrElse(exchange + "_events")))
  }
}

object ReefServicesList extends ServiceListOnMap(ReefServiceMap.servicemap)

