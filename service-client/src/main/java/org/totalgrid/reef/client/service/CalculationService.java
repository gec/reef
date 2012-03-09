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
import org.totalgrid.reef.client.service.proto.Calculations.Calculation;
import org.totalgrid.reef.client.service.proto.Model.ReefUUID;

import java.util.List;

/**
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface CalculationService
{
    /**
     * @return list of all of the calculations in the system
     */
    List<Calculation> getCalculations() throws ReefServiceException;

    /**
     * Get a particular calculation by its uuid
     * @param uuid calculation uuid
     * @return the calculation (or exception)
     */
    Calculation getCalculationByUuid( ReefUUID uuid ) throws ReefServiceException;

    /**
     * lookup a calculation associated with a point.
     * @param pointName name of the point (point should have type CalculatedPoint)
     * @return calculation or exception if doesn't exist
     */
    Calculation getCalculationForPointByName( String pointName ) throws ReefServiceException;

    /**
     * lookup a calculation associated with a point.
     * @param uuid uuid of the point (point should have type CalculatedPoint)
     * @return calculation or exception if doesn't exist
     */
    Calculation getCalculationForPointByUuid( ReefUUID uuid ) throws ReefServiceException;

    /**
     * calculations are associated with an endpoint and this allows us to get a list of all
     * calcs associated with that endpoint.
     * @param endpointName name of the endpoint
     * @return list of the calculations or an exception if the endpoint doesnt exist
     */
    List<Calculation> getCalculationsSourcedByEndpointByName( String endpointName ) throws ReefServiceException;

    /**
     * calculations are associated with an endpoint and this allows us to get a list of all
     * calcs associated with that endpoint.
     * @param uuid uuid of the endpoint
     * @return list of the calculations or an exception if the endpoint doesnt exist
     */
    List<Calculation> getCalculationsSourcedByEndpointByUuid( ReefUUID uuid ) throws ReefServiceException;

    /**
     * calculations are associated with an endpoint and this allows us to get a list of all
     * calcs associated with that endpoint.
     * @param uuid uuid of the endpoint
     * @return list of the calculations and a subscription to changes or an exception if the endpoint doesnt exist
     */
    SubscriptionResult<List<Calculation>, Calculation> subscribeToCalculationsSourcedByEndpointByUuid( ReefUUID uuid ) throws ReefServiceException;

    /**
     * add (or update) a new calculation
     * @param calculation calculation proto that needs to be fully populated and valid
     * @return the created calculation (with uuids filled in) or an exception
     */
    Calculation addCalculation( Calculation calculation ) throws ReefServiceException;

    /**
     * remove a calculation specified by uuid
     * @param uuid calculation uuid
     * @return the deleted calculation (or exception)
     */
    Calculation deleteCalculation( ReefUUID uuid ) throws ReefServiceException;

    /**
     * remove a calculation specified by uuid
     * @param calculation calculation proto object
     * @return the deleted calculation (or exception)
     */
    Calculation deleteCalculation( Calculation calculation ) throws ReefServiceException;

}
