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
package org.totalgrid.reef.proto

import org.totalgrid.reef.protoapi.{ ServiceListOnMap, ServiceInfo, ITypeDescriptor, ServiceList }

object ReefServiceMap {
  val servicemap: ServiceList.ServiceMap = Map(

    getEntry(Descriptors.port, "front_end_port"),
    getEntry(Descriptors.frontEndProcessor, "front_end_processor"),
    getEntry(Descriptors.communicationEndpointConfig, "comm_endpoint"),
    getEntry(Descriptors.communicationEndpointConnection, "front_end_assignment"),
    getEntry(Descriptors.measurementProcessingConnection, "meas_proc_assignment"),

    getEntry(Descriptors.measurementBatch, "measurement_batch"),
    getEntry(Descriptors.measurementHistory, "measurement_history"),
    getEntry(Descriptors.measurementSnapshot, "measurement_snapshot", Some(Descriptors.measurement), Some("measurement")),
    getEntry(Descriptors.measOverride, "meas_override"),
    getEntry(Descriptors.triggerSet, "trigger_set"),
    getEntry(Descriptors.statusSnapshot, "process_status"),

    getEntry(Descriptors.event, "event"),
    getEntry(Descriptors.eventList, "event_list"),
    getEntry(Descriptors.eventConfig, "event_config"),
    getEntry(Descriptors.alarm, "alarm"),
    getEntry(Descriptors.alarmList, "alarm_list"),
    getEntry(Descriptors.authToken, "auth_token"),

    getEntry(Descriptors.userCommandRequest, "user_command_request"),
    getEntry(Descriptors.commandAccess, "command_access"),

    getEntry(Descriptors.applicationConfig, "app_config"),

    getEntry(Descriptors.configFile, "config_file"),
    getEntry(Descriptors.command, "command"),
    getEntry(Descriptors.point, "point"),
    getEntry(Descriptors.entity, "entity"),
    getEntry(Descriptors.entityEdge, "entity_edge"))

  private def getEntry[A, B](descriptor: ITypeDescriptor[A], exchange: String, subClass: Option[ITypeDescriptor[B]] = None, subExchange: Option[String] = None): ServiceList.ServiceTuple = {
    (descriptor.getKlass -> ServiceInfo(
      exchange,
      descriptor,
      subClass.isDefined,
      subClass.getOrElse(descriptor),
      subExchange.getOrElse(exchange + "_events")))
  }
}

object ReefServicesList extends ServiceListOnMap(ReefServiceMap.servicemap) {
  def getInstance() = this
}

