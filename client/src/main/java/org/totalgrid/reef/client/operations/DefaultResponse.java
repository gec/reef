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
package org.totalgrid.reef.client.operations;

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.proto.Envelope;
import org.totalgrid.reef.client.proto.StatusCodes;

import java.util.List;

/**
 * Default implementation of the Response object. This class will generally only need to be used by service
 * implementors, not client applications. Responses can be either successful or failures.
 * @see Response
 */
public class DefaultResponse<A> implements Response<A>
{

    private final Envelope.Status status;
    private final String errorMessage;
    private final List<A> results;

    /**
     * Construct a failure response with bad status code and an errorMessage
     * @param status must be a non-2xx code
     * @param errorMessage descriptive string describing the error
     */
    public DefaultResponse( Envelope.Status status, String errorMessage )
    {
        if ( StatusCodes.isSuccess( status ) )
            throw new IllegalArgumentException( "Cannot construct failure response with a success error code" );
        this.status = status;
        this.errorMessage = errorMessage;
        this.results = null;
    }

    /**
     * Construct a success response with a list of result objects.
     * @param status must be a 2xx code
     * @param results list of response objects
     */
    public DefaultResponse( Envelope.Status status, List<A> results )
    {
        if ( !StatusCodes.isSuccess( status ) )
            throw new IllegalArgumentException( "Cannot construct success response with a failure error code" );
        this.status = status;
        this.errorMessage = "";
        this.results = results;
    }

    public boolean isSuccess()
    {
        return StatusCodes.isSuccess( status );
    }

    public List<A> getList()
    {
        return results;
    }

    public ReefServiceException getException()
    {
        if ( isSuccess() )
            throw new IllegalArgumentException( "Response was successful, no exception available" );
        return StatusCodes.toException( status, errorMessage );
    }

    public Envelope.Status getStatus()
    {
        return status;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }
}
