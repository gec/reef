package org.totalgrid.reef.api.service.async

import org.totalgrid.reef.api.RequestEnv

trait ServiceAsync[A <: AnyRef] extends IServiceAsync {

  def get(req: A, env: RequestEnv, callback: IServiceResponseCallback): Unit
  def put(req: A, env: RequestEnv, callback: IServiceResponseCallback): Unit
  def delete(req: A, env: RequestEnv, callback: IServiceResponseCallback): Unit
  def post(req: A, env: RequestEnv, callback: IServiceResponseCallback): Unit

}