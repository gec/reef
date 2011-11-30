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

import static org.junit.Assert.*;

import org.totalgrid.reef.client.exceptions.ReefServiceException;
import org.totalgrid.reef.client.service.CommandService;
import org.totalgrid.reef.client.service.PointService;
import org.totalgrid.reef.proto.Model.*;

import java.util.List;

import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestPointService extends ReefConnectionTestBase
{
    /** Test that some points names are returns from the Point service */
    @Test
    public void somePointsReturned() throws ReefServiceException
    {
        PointService ps = helpers;

        List<Point> allPoints = ps.getPoints();
        assertTrue( "expected at least one point: " + allPoints.size(), allPoints.size() > 0 );
    }

    /** Given a command, find the feedback point using the point service's entity query. */
    @Test
    public void commandFeedback() throws ReefServiceException
    {
        CommandService cs = helpers;
        PointService ps = helpers;
        // Get a command from the command service
        List<Command> commands = cs.getCommands();
        assertTrue( commands.size() > 0 );
        for ( Command cmd : commands )
        {
            List<Point> list = ps.getPointsThatFeedbackForCommand( cmd.getUuid() );
            assertNotNull( list );
            assertEquals( 1, list.size() );

            for ( Point p : list )
            {
                List<Command> roundtripCommands = cs.getCommandsThatFeedbackToPoint( p.getUuid() );
                assertTrue( roundtripCommands.contains( cmd ) );
            }
        }
    }

    @Test
    public void pointFeedback() throws ReefServiceException
    {
        CommandService cs = helpers;
        PointService ps = helpers;
        // Get a command from the command service
        List<Point> points = ps.getPoints();
        assertTrue( points.size() > 0 );
        for ( Point p : points )
        {
            List<Command> commands = cs.getCommandsThatFeedbackToPoint( p.getUuid() );
            assertNotNull( commands );

            for ( Command cmd : commands )
            {
                List<Point> roundtripPoints = ps.getPointsThatFeedbackForCommand( cmd.getUuid() );
                assertTrue( roundtripPoints.contains( p ) );
            }
        }
    }

}