package org.totalgrid.reef.services.framework

import org.totalgrid.reef.sapi.service.HasServiceType
import org.totalgrid.reef.sapi.client.Response
import org.totalgrid.reef.japi.Envelope

object SimpleServiceBehaviors {
  trait SimpleRead extends HasServiceType with AsyncContextRestGet with AuthorizesRead {

    override def getAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit) {
      authorizeRead(context, req)
      val result = doGet(context, req)
      subscribe(context, req)
      callback(Response(Envelope.Status.OK, result))
    }

    def subscribe(context: RequestContext, req: ServiceType) = {
      context.headers.subQueue.foreach { subQueue =>
        val keys = getSubscribeKeys(req)
        keys.foreach(context.subHandler.bind(subQueue, _, req))
      }
    }

    def getSubscribeKeys(req: ServiceType): List[String]

    def doGet(context: RequestContext, req: ServiceType): ServiceType
  }

}