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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.proto.Commands.UserCommandRequest
import org.totalgrid.reef.models.UserCommandModel

import ServiceBehaviors._

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.api.{ Envelope, BadRequestException }

class UserCommandRequestServiceBackup(
  protected val modelTrans: ServiceTransactable[UserCommandRequestServiceModel])
    extends SyncModeledServiceBase[UserCommandRequest, UserCommandModel, UserCommandRequestServiceModel]
    with GetEnabled
    with SubscribeEnabled
    with PutPostEnabled
    with DeleteDisabled {

  override val descriptor = Descriptors.userCommandRequest

  private def doCommonValidation(proto: UserCommandRequest) = {

    if (!proto.hasCommandRequest)
      throw new BadRequestException("Request must specify command information", Envelope.Status.BAD_REQUEST)

    proto
  }

  override protected def preCreate(proto: UserCommandRequest) = {

    if (!proto.getCommandRequest.hasName)
      throw new BadRequestException("Request must specify command name", Envelope.Status.BAD_REQUEST)

    if (proto.hasStatus)
      throw new BadRequestException("Update must not specify status", Envelope.Status.BAD_REQUEST)

    this.doCommonValidation(proto)
  }

  override protected def preUpdate(proto: UserCommandRequest, existing: UserCommandModel) = {

    if (!proto.hasStatus)
      throw new BadRequestException("Update must specify status", Envelope.Status.BAD_REQUEST)

    doCommonValidation(proto)
  }

}

object UserCommandRequestServiceBackup {
  val defaultTimeout = 30000
}