package org.totalgrid.reef.api.service.async

import org.totalgrid.reef.api.{ Envelope, RequestEnv }

/**
 * Defines how to complete a service call with a ServiceResponse
 */

trait IServiceAsync {
  def respond(req: Envelope.ServiceRequest, env: RequestEnv, callback: IServiceResponseCallback): Unit
}

