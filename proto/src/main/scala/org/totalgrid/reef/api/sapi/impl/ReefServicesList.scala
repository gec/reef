package org.totalgrid.reef.api.sapi.impl

import org.totalgrid.reef.api.japi.TypeDescriptor
import org.totalgrid.reef.api.sapi.{ ServiceListOnMap, ServiceInfo, ServiceList }

object ReefServiceMap {
  val servicemap: ServiceList.ServiceMap = Map(

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
    getEntry(Descriptors.entityAttributes))

  private def getEntry[A, B](descriptor: TypeDescriptor[A], subClass: Option[TypeDescriptor[B]] = None): ServiceList.ServiceTuple = subClass match {
    case Some(subDescriptor) => descriptor.getKlass -> ServiceInfo(descriptor, subDescriptor)
    case None => descriptor.getKlass -> ServiceInfo(descriptor)
  }
}

object ReefServicesList extends ServiceListOnMap(ReefServiceMap.servicemap) {
  def getInstance(): ServiceList = this
}

