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
package org.totalgrid.reef.client.service;

import org.totalgrid.reef.clientapi.exceptions.ReefServiceException;
import org.totalgrid.reef.proto.FEP.Endpoint;
import org.totalgrid.reef.proto.FEP.CommChannel;
import org.totalgrid.reef.proto.FEP.CommChannel.State;
import org.totalgrid.reef.proto.Model.ReefUUID;

import java.util.List;

/**
 * In reef a communication channel is the representation of the "low-level" connection to an external resource
 * like a serial port or tcp socket.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface CommunicationChannelService
{
    /**
     *
     * @return list of all of the communication channels
     */
    List<CommChannel> getCommunicationChannels() throws ReefServiceException;

    /**
     * @param channelUuid uuid of channel
     * @return channel with matching uuid or exception if doesn't exist
     */
    CommChannel getCommunicationChannelByUuid( ReefUUID channelUuid ) throws ReefServiceException;

    /**
     * @param channelName name of the channel
     * @return channel with matching name or exception if doesn't exist
     */
    CommChannel getCommunicationChannelByName( String channelName ) throws ReefServiceException;

    /**
     * Protocol adapters will use this call to
     *
     * @param channelUuid  uuid of channel we are updating
     * @param state        state we are setting it to
     * @return updated CommChannel object
     */
    CommChannel alterCommunicationChannelState( ReefUUID channelUuid, State state ) throws ReefServiceException;

    /**
     * get a list of all endpoints that use a specific channel
     * @param channelUuid
     * @return list of endpoints using this channel
     */
    List<Endpoint> getEndpointsUsingChannel( ReefUUID channelUuid ) throws ReefServiceException;
}
