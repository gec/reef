/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.framework

import org.totalgrid.reef.api.service.{ AsyncToSyncServiceAdapter, AsyncServiceBase }

/**
 * Shared dependencies for generic service implementations
 */
trait ModeledService extends HasServiceModelType {

  protected val modelTrans: ServiceTransactable[ServiceModelType]

}

trait HasSubscribe extends HasServiceModelType {

  def subscribe(model: ServiceModelType, req: ServiceType, queue: String)

}

/**
 * Base class for services which handle protobuf messages and act on service models.
 *
 * Implements SyncServiceBase/ProtoSyncServiceBase interfaces to the messaging system
 * and provides shared types/resource definitions for mixed-in service behavior.
 */
trait ModeledServiceBase[ST <: AnyRef, MT, SMT <: ServiceModel[ST, MT]] extends ServiceModelTypeIs[ST, MT, SMT] with ServiceTypeIs[ST] with ModelTypeIs[MT]

trait AsyncModeledServiceBase[ST <: AnyRef, MT, SMT <: ServiceModel[ST, MT]] extends ModeledServiceBase[ST, MT, SMT] with AsyncServiceBase[ST]

trait SyncModeledServiceBase[ST <: AnyRef, MT, SMT <: ServiceModel[ST, MT]] extends ModeledServiceBase[ST, MT, SMT] with AsyncToSyncServiceAdapter[ST]

