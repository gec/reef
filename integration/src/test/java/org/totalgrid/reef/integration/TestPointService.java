/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/agpl.html
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

import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.api.japi.request.CommandService;
import org.totalgrid.reef.api.japi.request.PointService;
import org.totalgrid.reef.api.proto.Model.*;

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

        List<Point> allPoints = ps.getAllPoints();
        assertTrue( "expected at least one point: " + allPoints.size(), allPoints.size() > 0 );
    }

    /** Test point searching by entity */
    @Test
    public void pointsInSubstation() throws ReefServiceException
    {
        // Specify all point entities that are under equipment
        Entity eqRequest =
            Entity.newBuilder().addTypes( "Substation" ).addRelations(
                    Relationship.newBuilder().setRelationship( "owns" ).setDescendantOf( true ).addEntities(
                            Entity.newBuilder().addTypes( "Equipment" ).addRelations(
                                    Relationship.newBuilder().setRelationship( "owns" ).setDescendantOf( true ).addEntities(
                                            Entity.newBuilder().addTypes( "Point" ) ) ) ) ).build();

        // Build the point service request using the entity descriptor
        Point p = Point.newBuilder().setEntity( eqRequest ).build();
        List<Point> list = client.get( p ).await().expectMany();

        assertNotNull( list );
        assertTrue( "expected at least one point: " + list.size(), list.size() > 0 );
        /*
         * // Verify that all retrieved points start their full name with "Apex" for(Point pt :
         * list) { String[] names = pt.getName().split("\\."); assert(names[0] == "Apex"); }
         */
    }

    /** Given a command, find the feedback point using the point service's entity query. */
    @Test
    public void pointFeedback() throws ReefServiceException
    {
        CommandService cs = helpers;
        // Get a command from the command service
        List<Command> commands = cs.getCommands();
        assertTrue( commands.size() > 0 );
        Command cmd = commands.get( 0 );

        // Use the entity of the command to get the feedback point
        Entity eqRequest =
            Entity.newBuilder( cmd.getEntity() ).addRelations(
                    Relationship.newBuilder().setRelationship( "feedback" ).setDescendantOf( false ).addEntities(
                            Entity.newBuilder().addTypes( "Point" ) ) ).build();

        // Build the point service request using the entity descriptor
        Point p = Point.newBuilder().setEntity( eqRequest ).build();
        List<Point> list = client.get( p ).await().expectMany();

        assertNotNull( list );
        assertEquals( 1, list.size() );
    }

}