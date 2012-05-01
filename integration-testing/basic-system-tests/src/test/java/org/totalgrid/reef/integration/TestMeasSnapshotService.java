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
package org.totalgrid.reef.integration;

import org.junit.*;

import static org.junit.Assert.assertEquals;

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.service.MeasurementService;
import org.totalgrid.reef.client.service.PointService;
import org.totalgrid.reef.client.service.proto.Measurements.*;
import org.totalgrid.reef.client.service.proto.Model.*;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class TestMeasSnapshotService extends ReefConnectionTestBase
{

    /**
     * Test that the number of measurements retrieved from the measurement Snapshot service by name
     * is the same.
     */
    @Test
    public void measSnapshotCountMatches() throws ReefServiceException
    {
        PointService ps = helpers;
        MeasurementService ms = helpers;
        List<Point> plist = ps.getPoints();
        List<Measurement> mlist = ms.getMeasurementsByPoints( plist );
        assertEquals( plist.size(), mlist.size() );

        List<ReefUUID> uuidList = new ArrayList<ReefUUID>( plist.size() );
        for ( Point p : plist )
        {
            uuidList.add( p.getUuid() );
        }
        List<Measurement> mlist2 = ms.getMeasurementsByUuids( uuidList );
        assertEquals( plist.size(), mlist2.size() );

    }


}
