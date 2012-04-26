package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.sapi.client.rest.ServiceRegistry
import org.totalgrid.reef.client.types.ServiceTypeInformation

trait UsesServiceRegistry {
  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _]
}
