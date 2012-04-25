package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.operations.ServiceOperations

abstract class ServiceOperationsProvider(protected val ops: ServiceOperations) extends ServiceOperationsAccess {

}
