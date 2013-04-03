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

import java.util.List;

/**
 * when a request to the server is made the result will always be returned to the client code wrapped
 * in a Response object.
 *
 * Generally isSuccess should be called first and then we either process the resultant list or throw
 * the error exception.
 */
public interface Response<T>
{

    /**
     * if success it means the result list will be set with result from the server, otherwise there was an error
     * and the error exception should be thrown.
     * @return if the response code from the server is any sort of success code (2xx)
     */
    boolean isSuccess();

    /**
     * all requests return a list by default, we check later that the correct number of results were retrieved
     * @return the list of server response objects if isSuccess == true, otherwise a blank list (but a success may also
     *         result in an empty list).
     */
    List<T> getList();

    /**
     * if (isSuccess) returns a correctly typed and formatted exception constructed using
     * the status code and errorMessage string.
     * @return error message.
     * @throws IllegalArgumentException if called when isSuccess == true
     */
    ReefServiceException getException();

    /**
     * detailed status code for response
     */
    Envelope.Status getStatus();

    /**
     * if there was an error processing the request (!isSuccess) the server will return a helpful
     * error message string
     * @return helpful error string if !isSuccess
     */
    String getErrorMessage();
}
