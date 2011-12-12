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

import org.totalgrid.reef.client.exception.ReefServiceException;

import java.util.List;

/**
 * TODO: document RESTOperations
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface RESTOperations
{

    /**
     *
     * @param request
     * @param <T>
     * @return
     * @throws ReefServiceException
     */
    public <T> T getOne( T request ) throws ReefServiceException;

    /**
     *
     * @param request
     * @param <T>
     * @return
     * @throws ReefServiceException
     */
    public <T> T findOne( T request ) throws ReefServiceException;

    /**
     *
     * @param request
     * @param <T>
     * @return
     * @throws ReefServiceException
     */
    public <T> List<T> getMany( T request ) throws ReefServiceException;

    public <T> T deleteOne( T request ) throws ReefServiceException;

    public <T> List<T> deleteMany( T request ) throws ReefServiceException;

    public <T> T putOne( T request ) throws ReefServiceException;

    public <T> List<T> putMany( T request ) throws ReefServiceException;

    public <T> T postOne( T request ) throws ReefServiceException;

    public <T> List<T> postMany( T request ) throws ReefServiceException;

}
