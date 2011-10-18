package org.totalgrid.reef.api.sapi

import org.totalgrid.reef.api.japi.TypeDescriptor

object TypeDescriptorHelpers {
  def getEventExchange[A](desc: TypeDescriptor[A]) = desc.id + "_events"
}