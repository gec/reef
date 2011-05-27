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
package org.totalgrid.reef.japi.request;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.proto.Model.Entity;
import org.totalgrid.reef.proto.Model.Point;
import org.totalgrid.reef.proto.Model.ReefUUID;

import java.util.List;

/**
 * A Point represents a configured input point for data acquisition. Measurements associated with this
 * point all use the point name and id. Once obtaining a Point object you should use the MeasurementService
 * to read/subscribe to the measurements for that point.
 * <p/>
 * Every Point is associated with an Entity of type "Point". The point's location in the system
 * model is determined by this entity. Points are also associated with entities designated as
 * "logical nodes", which represent the communications interface/source.
 */
public interface PointService
{

    /**
     * get all points in the system
     *
     * @return all points
     */
    List<Point> getAllPoints();

    /**
     * retrieve a point by name, throws exception if point is unknown
     *
     * @param name of the Point we are retrieving
     * @return the point object with matching name
     */
    Point getPointByName( String name ) throws ReefServiceException;

    /**
     * retrieve a point by uuid, throws exception if point is unknown
     *
     * @param uuid of the Point we are retrieving
     * @return the point object with matching name
     */
    Point getPointByUid( ReefUUID uuid ) throws ReefServiceException;

    /**
     * retrieve all points that are have the relationship "owns" to the parent entity
     *
     * @param parentEntity parent we are looking for children of
     * @return points owned by parentEntity
     */
    List<Point> getPointsOwnedByEntity( Entity parentEntity ) throws ReefServiceException;

    /**
     * retrieve all points that are have the relationship "source" to the endpoint
     *
     * @param endpointUuid uuid of endpoint
     * @return all points that are related to endpoint
     */
    List<Point> getPointsBelongingToEndpoint( ReefUUID endpointUuid ) throws ReefServiceException;

}