package org.totalgrid.reef.client.sapi

import org.totalgrid.reef.api.japi.TypeDescriptor
import org.totalgrid.reef.api.sapi.types.ServiceInfo

object ReefServicesList {
  def getServicesList = List(
    getEntry(Descriptors.commChannel),
    getEntry(Descriptors.frontEndProcessor),
    getEntry(Descriptors.commEndpointConfig),
    getEntry(Descriptors.commEndpointConnection),
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
    getEntry(Descriptors.commandAccess),

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
    case Some(subDescriptor) => ServiceInfo(descriptor, subDescriptor)
    case None => ServiceInfo(descriptor)
  }
}

