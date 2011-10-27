/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.proto.Model.ReefUUID

import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig, CommChannel }

import org.totalgrid.reef.client.sapi.rpc.CommunicationChannelService
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.HasAnnotatedOperations

trait CommunicationChannelServiceImpl extends HasAnnotatedOperations with CommunicationChannelService {

  override def getAllCommunicationChannels = ops.operation("Couldn't get list of all channels") {
    _.get(CommChannel.newBuilder().setName("*").build).map(_.many)
  }

  override def getCommunicationChannelByName(channelName: String) = ops.operation("Couldn't get channel with name: " + channelName) {
    _.get(CommChannel.newBuilder().setName(channelName).build).map(_.one)
  }

  override def getCommunicationChannel(channelUuid: ReefUUID) = ops.operation("Couldn't get channel with uuid: " + channelUuid) {
    _.get(CommChannel.newBuilder().setUuid(channelUuid).build).map(_.one)
  }

  override def alterCommunicationChannelState(channelUuid: ReefUUID, state: CommChannel.State) = {
    ops.operation("Couldn't alter channel: " + channelUuid.getUuid + " to : " + state) {
      _.post(CommChannel.newBuilder.setUuid(channelUuid).setState(state).build).map(_.one)
    }
  }

  override def getEndpointsUsingChannel(channel: ReefUUID) = {
    ops.operation("Can't find endpoints for channel uuid: " + channel.getUuid) {
      _.get(CommEndpointConfig.newBuilder.setUuid(channel).build).map(_.many)
    }
  }
}