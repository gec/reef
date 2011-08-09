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

import org.totalgrid.reef.sapi.service._
import org.totalgrid.reef.sapi.client.Response

/**
 * defines the get verb and provides a default fail operation
 */
trait AsyncContextRestGet extends HasServiceType {
  def getAsync(context: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noGet[ServiceType])
}

trait AsyncContextRestDelete extends HasServiceType {
  def deleteAsync(context: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noDelete[ServiceType])
}

trait AsyncContextRestPost extends HasServiceType {
  def postAsync(context: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noPost[ServiceType])
}

trait AsyncContextRestPut extends HasServiceType {
  def putAsync(context: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noPut[ServiceType])
}

/**
 * rollup trait that has default implementations for all 4 verbs
 */
trait AsyncContextRestService extends AsyncContextRestGet with AsyncContextRestDelete with AsyncContextRestPost with AsyncContextRestPut

/**
 * all services that we will use with ServiceMiddleware will implement this trait
 */
trait ServiceEntryPoint[A <: AnyRef] extends ServiceTypeIs[A] with ServiceDescriptor[A] with AsyncContextRestService

