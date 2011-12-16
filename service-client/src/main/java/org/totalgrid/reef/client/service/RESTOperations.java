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

import org.totalgrid.reef.client.SubscriptionResult;
import org.totalgrid.reef.client.exception.ReefServiceException;

import java.util.List;

/**
 * All of the calls to the reef server are implemented by using one of the 4 verbs (GET, PUT, DELETE, POST)
 * and a protobuf object that serves as a request. The service APIs provided cover 95% of the use cases we
 * expect applications to use but if a particular query is missing from the API we want to provide a way to
 * send a custom query to the server. This allows a quick way for an extra request to be implemented for one
 * application with needing to wait for an update to this package.
 *
 * Most clients will not need to use this interface so it has intentionally been left out of the big "rollup"
 * traits. This interface can be thought of as the "low-level" interface to reef, an application could be
 * constructed entirely using these sorts of queries but it would be much harder than using the semantic APIs.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface RESTOperations
{

    /**
     * make a GET request assuming there will only be one result, throws if zero or more than 1
     */
    public <T> T getOne( T request ) throws ReefServiceException;

    /**
     * make a GET request assuming there will only be zero or one result, throws if more than 1, returns null if 0
     */
    public <T> T findOne( T request ) throws ReefServiceException;

    /**
     * make a GET request and dont assume how many results will be returned, return a list which may be empty
     */
    public <T> List<T> getMany( T request ) throws ReefServiceException;

    /**
     * same as getMany but sets up a subscription as well
     */
    public <T> SubscriptionResult<List<T>, T> subscribeMany( T request ) throws ReefServiceException;

    /**
     * make a DELETE request assuming there will only be one result, throws if zero or more than 1
     */
    public <T> T deleteOne( T request ) throws ReefServiceException;

    /**
     * make a DELETE request and dont assume how many results will be returned, return a list which may be empty
     */
    public <T> List<T> deleteMany( T request ) throws ReefServiceException;

    /**
     * make a PUT request assuming there will only be one result, throws if zero or more than 1
     */
    public <T> T putOne( T request ) throws ReefServiceException;

    /**
     * make a PUT request and dont assume how many results will be returned, return a list which may be empty
     */
    public <T> List<T> putMany( T request ) throws ReefServiceException;

    /**
     * make a POST request assuming there will only be one result, throws if zero or more than 1
     */
    public <T> T postOne( T request ) throws ReefServiceException;

    /**
     * make a POST request and dont assume how many results will be returned, return a list which may be empty
     */
    public <T> List<T> postMany( T request ) throws ReefServiceException;

}
