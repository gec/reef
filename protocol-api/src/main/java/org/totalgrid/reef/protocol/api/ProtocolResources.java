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
package org.totalgrid.reef.protocol.api;

import org.totalgrid.reef.client.Client;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.service.proto.FEP;
import org.totalgrid.reef.client.service.proto.Measurements;
import org.totalgrid.reef.client.service.proto.Model;

import java.util.List;

/**
 * Utility methods for protocol-related operations.
 */
public interface ProtocolResources
{

    /**
     * Exposes the client object associated with this resources object.
     *
     * @return Client object
     */
    Client getClient();

    /**
     * Exposes the endpoint configuration associated with this resources object.
     *
     * @return Endpoint description
     */
    FEP.EndpointConnection getEndpointConnection();

    /**
     * Name of the endpoint.
     *
     * @return Name of the endpoint
     */
    String getEndpointName();

    /**
     * Searches ConfigFile objects associated with the endpoint by mime-type.
     *
     * @param mimeType Mime-type of config file to find
     * @return ConfigFile object
     * @throws ReefServiceException
     */
    Model.ConfigFile getConfigFile( String mimeType ) throws ReefServiceException;

    /**
     * Publish a list of measurements to the stream associated with the endpoint.
     *
     * @param measurementList
     * @throws ReefServiceException
     */
    void publishMeasurements( List<Measurements.Measurement> measurementList ) throws ReefServiceException;

    /**
     * Update the system about the state of the endpoint.
     *
     * @param state New communications state
     * @throws ReefServiceException
     */
    void setCommsState( FEP.EndpointConnection.State state ) throws ReefServiceException;

    /**
     * Whether the endpoint has a communication channel associated with it.
     *
     * @return
     */
    boolean hasCommChannel();

    /**
     * The communication channel associated with the endpoint, null if none exists.
     *
     * @return
     */
    FEP.CommChannel getCommChannel();

    /**
     * Update the system about the state of the communication channel.
     *
     * @param state New comm channel state
     * @throws ReefServiceException
     */
    void setChannelState( FEP.CommChannel.State state ) throws ReefServiceException;

}