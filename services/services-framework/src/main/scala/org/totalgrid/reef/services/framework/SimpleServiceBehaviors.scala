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

import org.totalgrid.reef.client.sapi.service.HasServiceType
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.operations.scl.ScalaRequestHeaders._
import org.totalgrid.reef.client.operations.scl.ScalaResponse

object SimpleServiceBehaviors {
  trait SimpleReadAndSubscribe extends HasServiceType with AsyncContextRestGet {

    final override def getAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit) {
      val response = contextSource.transaction { context =>
        val result = doGetAndSubscribe(context, req)
        ScalaResponse.success(Envelope.Status.OK, result)
      }
      callback(response)
    }

    protected def subscribe(context: RequestContext, keys: => List[String], klass: Class[_]) = {
      context.getHeaders.subQueue.foreach { subQueue =>
        keys.foreach(context.eventPublisher.bindQueueByClass(subQueue, _, klass))
      }
    }

    def doGetAndSubscribe(context: RequestContext, req: ServiceType): ServiceType
  }

  trait SimpleRead extends SimpleReadAndSubscribe {

    def getSubscribeKeys(req: ServiceType): List[String]

    def subscriptionClass: Class[_]

    def doGet(context: RequestContext, req: ServiceType): ServiceType

    final override def doGetAndSubscribe(context: RequestContext, req: ServiceType): ServiceType = {
      subscribe(context, getSubscribeKeys(req), subscriptionClass)
      doGet(context, req)
    }
  }

  trait SimplePost extends HasServiceType with AsyncContextRestPost {

    override def postAsync(source: RequestContextSource, req: ServiceType)(callback: (Response[ServiceType]) => Unit) {
      val response = source.transaction { context =>
        val result = doPost(context, req)
        ScalaResponse.success(Envelope.Status.OK, result)
      }
      callback(response)
    }

    def doPost(context: RequestContext, req: ServiceType): ServiceType
  }

}