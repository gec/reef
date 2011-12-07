package org.totalgrid.reef.client.service.list

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
import org.totalgrid.reef.client.types.{ ServiceInfo, TypeDescriptor, ServiceTypeInformation }
import org.totalgrid.reef.client.service.proto.Descriptors

object ReefServicesList {
  import scala.collection.JavaConversions._
  def getServicesList: java.util.List[ServiceTypeInformation[_, _]] = List(
    getEntry(Descriptors.commChannel),
    getEntry(Descriptors.frontEndProcessor),
    getEntry(Descriptors.endpoint),
    getEntry(Descriptors.endpointConnection),
    getEntry(Descriptors.measurementProcessingConnection),

    getEntry(Descriptors.measurementBatch),
    getEntry(Descriptors.measurementHistory, Some(Descriptors.measurement)),
    getEntry(Descriptors.measurementSnapshot, Some(Descriptors.measurement)),
    getEntry(Descriptors.measOverride),
    getEntry(Descriptors.triggerSet),
    getEntry(Descriptors.statusSnapshot),

    getEntry(Descriptors.event),
    getEntry(Descriptors.eventList),
    getEntry(Descriptors.eventConfig),
    getEntry(Descriptors.alarm),
    getEntry(Descriptors.alarmList),
    getEntry(Descriptors.authToken),
    getEntry(Descriptors.agent),
    getEntry(Descriptors.permissionSet),

    getEntry(Descriptors.userCommandRequest),
    getEntry(Descriptors.commandLock),

    getEntry(Descriptors.applicationConfig),

    getEntry(Descriptors.configFile),
    getEntry(Descriptors.command),
    getEntry(Descriptors.point),
    getEntry(Descriptors.entity),
    getEntry(Descriptors.entityEdge),
    getEntry(Descriptors.entityAttributes),
    // TODO: we only need this here to get event publishing to work
    getEntry(Descriptors.measurement))

  private def getEntry[A, B](descriptor: TypeDescriptor[A], subClass: Option[TypeDescriptor[B]] = None) = subClass match {
    case Some(subDescriptor) => new ServiceInfo(descriptor, subDescriptor)
    case None => new ServiceInfo(descriptor, descriptor)
  }
}

