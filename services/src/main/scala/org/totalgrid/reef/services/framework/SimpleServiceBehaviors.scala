/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.framework

import org.totalgrid.reef.sapi.service.HasServiceType
import org.totalgrid.reef.sapi.client.Response
import org.totalgrid.reef.japi.Envelope

object SimpleServiceBehaviors {
  trait SimpleRead extends HasServiceType with AsyncContextRestGet with AuthorizesRead {

    override def getAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit) {
      val response = contextSource.transaction { context =>
        authorizeRead(context, req)
        val result = doGet(context, req)
        subscribe(context, req)
        Response(Envelope.Status.OK, result)
      }
      callback(response)
    }

    def subscribe(context: RequestContext, req: ServiceType) = {
      context.getHeaders.subQueue.foreach { subQueue =>
        val keys = getSubscribeKeys(req)
        keys.foreach(context.subHandler.bind(subQueue, _, req))
      }
    }

    def getSubscribeKeys(req: ServiceType): List[String]

    def doGet(context: RequestContext, req: ServiceType): ServiceType
  }

}