/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request;

import org.totalgrid.reef.api.ReefServiceException;
import org.totalgrid.reef.api.javaclient.SubscriptionCreator;
import org.totalgrid.reef.api.javaclient.SubscriptionResult;
import org.totalgrid.reef.proto.Measurements.Measurement;
import org.totalgrid.reef.proto.Model.Point;

import java.util.List;

/**
 * Non-exhaustive API for using the reef Measurement services. This API allows the client code to read current measurement
 * values for many points at a time, read historical values for a single measuremnt at a time or publish measurements in
 * batches. For current and historical value functions you can also pass in an Subscription object which will receive
 * all future measurement changes for those points. Asking for unknown points/ measurements will result in an exception
 */
public interface MeasurementService extends SubscriptionCreator {

    /**
     * gets the most recent measurement for a point
     */

    public Measurement getMeasurementByPoint(Point point) throws ReefServiceException;

    /**
     * gets the current value for a point (specified by name)
     */

    public Measurement getMeasurementByName(String name) throws ReefServiceException;

    /**
     * gets the most recent measurement for a set of points. If any points are unknown the call will throw a bad request
     * exception.
     */

    public List<Measurement> getMeasurementsByNames(List<String> names) throws ReefServiceException;

    /**
     * gets the most recent measurement for a set of points (specified by names). If any points are unknown the
     * call will throw a bad request exception.
     */

    public List<Measurement> getMeasurementsByPoints(List<Point> points) throws ReefServiceException;

    /**
     * gets the most recent measurement for a set of points and configure a subscription to receive updates for every
     * measurement change
     */

    public SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementsByPoints(List<Point> points) throws ReefServiceException;

    /**
     * gets the most recent measurement for a set of points and configure a subscription to receive updates for every
     * measurement change
     */

    public SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementsByNames(List<String> points) throws ReefServiceException;

    /**
     * get a subset of the recent measurements for a point
     *
     * @param limit - max number of measurements returned
     */

    public List<Measurement> getMeasurementHistory(Point point, int limit) throws ReefServiceException;

    /**
     * get a subset of the recent measurements for a point
     *
     * @param since - dont return measurements older than this, inclusive (millis)
     * @param limit - max number of measurements returned
     */

    public List<Measurement> getMeasurementHistory(Point point, long since, int limit) throws ReefServiceException;

    /**
     * get a subset of the recent measurements for a point. Note that setting the endTime and subscribing is incompatible
     * and will result in an exception since its dangerous to ask for a fixed time period and also be getting new
     * measurements since this can lead to missed measurements.
     *
     * @param since        - don't return measurements older than this, inclusive (millis)
     * @param before       - don't return measurements newer than this, inclusive (millis)
     * @param returnNewest - if there are more measurements in the range the range than limit return the newest measurements
     * @param limit        - max number of measurements returned
     */

    public List<Measurement> getMeasurementHistory(Point point, long since, long before, boolean returnNewest, int limit) throws ReefServiceException;

    /**
     * get the most recent measurements for a point and setup a subscription for new measurements
     *
     * @param limit - max number of measurements returned
     */

    public SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementHistory(Point point, int limit) throws ReefServiceException;

    /**
     * get the most recent measurements for a point and setup a subscription for new measurements
     *
     * @param since - don't return measurements older than this, inclusive (millis)
     * @param limit - max number of measurements returned
     */

    public SubscriptionResult<List<Measurement>, Measurement> subscribeToMeasurementHistory(Point point, long since, int limit) throws ReefServiceException;

    /**
     * publish a batch of measurements as if the client was a protocol adapter. Can fail for many reasons and most clients
     * should not use this function. If any point is not publishable the whole group should fail.
     * Preconditions for  success*   - the points listed in the measurements all need to exist
     * - the points must be configured to use an appropriate protocol (benchmark or manual) to maintain message stream
     * implement TODO protocol checking on publishMeasurements
     * - measurement processors must be available to process the measurement (only question during startup)
     */

    public void publishMeasurements(List<Measurement> measurements) throws ReefServiceException;
}
