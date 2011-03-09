package org.totalgrid.reef.api.service.async

import org.totalgrid.reef.api.{ Envelope, RequestEnv }

/**
 * Defines how to complete a service call with a ServiceResponse
 */
trait IServiceResponseCallback {
  def onResponse(rsp: Envelope.ServiceResponse)
}

