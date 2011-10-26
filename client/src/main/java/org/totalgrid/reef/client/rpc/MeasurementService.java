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
package org.totalgrid.reef.client.rpc;

import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.api.japi.client.Routable;
import org.totalgrid.reef.api.japi.client.SubscriptionCreator;
import org.totalgrid.reef.api.japi.client.SubscriptionResult;
import org.totalgrid.reef.proto.Measurements.Measurement;
import org.totalgrid.reef.proto.Measurements.MeasurementBatch;
import org.totalgrid.reef.proto.Model.Point;

import java.util.List;

/**
 * <p>
 *   Service for retrieving, subscribing to, and publishing measurements. Clients can retrieve current measurement
 *   values for multiple points, read historical values for a single point, or
 *   publish measurements in batches.
 *   </p>
 *
 * <p>
 *   Asking for unknown points will result in an exception.
 *   </p>
 */
public interface MeasurementService extends SubscriptionCreator
{

    /**
     * Get the most recent measurement for a point.
     */
    Measurement getMeasurementByPoint( Point point ) throws ReefServiceException;

    /**
     * Get the most recent measurement for a point.
     */
    Measurement getMeasurementByName( String pointName ) throws ReefServiceException;

    /**
     * Find the most recent measurement for a point, returning null if the measurement is unknown
     */
    Measurement findMeasurementByName( String pointName ) throws ReefServiceException;

    /**
     * Get the most recent measurement for a set of points. If any points are unknown,
     * the call will throw a bad request exception.
     */
    List<Measurement> getMeasurementsByNames( List<String> pointNames ) throws ReefServiceException;

    /**
     * Get the most recent measurement for a set of points. If any points are unknown, the
     * call will throw a bad request exception.
     */
    List<Measurement> getMeasurementsByPoints( List<Point> points ) throws ReefServiceException;

    /**
     * Get the most recent measurement for a set of points and subscribe to receive updates for
     * measurement changes.
     */
    SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementsByPoints( List<Point> points ) throws ReefServiceException;

    /**
     * Gets the most recent measurement for a set of points and subscribe to receive updates for
     * measurement changes.
     */
    SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementsByNames( List<String> pointNames ) throws ReefServiceException;

    /**
     * Get a list of recent measurements for a point.
     *
     * @param limit  Max number of measurements returned
     */
    List<Measurement> getMeasurementHistory( Point point, int limit ) throws ReefServiceException;

    /**
     * Get a list of historical measurements that were recorded on or after the specified time.
     *
     * @param since  Return measurements on or after this date/time (in milliseconds).
     * @param limit  max number of measurements returned
     */
    List<Measurement> getMeasurementHistory( Point point, long since, int limit ) throws ReefServiceException;

    /**
     * Get a list of historical measurements for the specified time span.
     *
     * @param from         Return measurements on or after this time (milliseconds)
     * @param to           Return measurements on or before this time (milliseconds)
     * @param returnNewest If there are more measurements than the specified limit, return the newest (true) or oldest (false).
     * @param limit        Max number of measurements returned
     */
    List<Measurement> getMeasurementHistory( Point point, long from, long to, boolean returnNewest, int limit ) throws ReefServiceException;

    /**
     * Get the most recent measurements for a point and subscribe to receive updates for
     * measurement changes.
     *
     * @param limit  Max number of measurements returned
     */
    SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementHistory( Point point, int limit ) throws ReefServiceException;

    /**
     * Get the most recent measurements for a point and subscribe to receive updates for
     * measurement changes.
     *
     * @param since  Return measurements on or after this time (milliseconds)
     * @param limit  Max number of measurements returned
     */
    SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementHistory( Point point, long since, int limit )
        throws ReefServiceException;

    /**
     * Publish a batch of measurements as if the client was a protocol adapter. Can fail for many reasons and most clients
     * should not use this function. If any point is not publishable, the whole group will fail.
     *
     * <p>Preconditions for  success:</p>
     * <ul>
     *   <li>Every point listed in the measurements exists</li>
     *   <li>The points must be configured to use an appropriate protocol (benchmark or manual) to maintain the message stream</li>
     *   <li>Measurement processors must be available to process the measurement (issue for system startup)</li>
     * </ul>
     * TODO: protocol checking on publishMeasurements
     */
    boolean publishMeasurements( List<Measurement> measurements ) throws ReefServiceException;

    /**
     * Publish a batch of measurements as if the client was a protocol adapter. Can fail for many reasons and most clients
     * should not use this function. If any point is not publishable, the whole group will fail.
     *
     * <p>Preconditions for  success:</p>
     * <ul>
     *   <li>Every point listed in the measurements exists</li>
     *   <li>The points must be configured to use an appropriate protocol (benchmark or manual) to maintain the message stream</li>
     *   <li>Measurement processors must be available to process the measurement (issue for system startup)</li>
     * </ul>
     */
    boolean publishMeasurements( MeasurementBatch batch, Routable destination ) throws ReefServiceException;
}
