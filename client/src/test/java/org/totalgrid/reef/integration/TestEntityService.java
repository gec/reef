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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.client.rpc.EntityService;
import org.totalgrid.reef.client.rpc.PointService;
import org.totalgrid.reef.proto.Model.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestEntityService extends ReefConnectionTestBase
{

    private interface IKeyGen<T>
    {
        public String getKey( T value );
    }

    private static <T> Map<String, T> toMap( List<T> list, IKeyGen<T> gen )
    {
        Map<String, T> map = new HashMap<String, T>();
        for ( T value : list )
        {
            map.put( gen.getKey( value ), value );
        }
        return map;
    }

    private Map<String, Entity> getEntityMap( List<Entity> list )
    {
        return toMap( list, new IKeyGen<Entity>() {
            public String getKey( Entity e )
            {
                return e.getName();
            }
        } );
    }

    private Map<String, Point> getPointMap( List<Point> list )
    {
        return toMap( list, new IKeyGen<Point>() {
            public String getKey( Point p )
            {
                return p.getName();
            }
        } );
    }

    /**
     * Ask for all of the entities in the system. You wouldn't want to actually do this in a
     * production system, because the result set could very large.
     * 
     * */
    @Test
    public void getAllEntities() throws ReefServiceException
    {
        EntityService es = helpers;
        List<Entity> list = es.getAllEntitiesWithType( "*" );
        assertTrue( list.size() > 0 ); // the number here is arbitrary

    }

    /**
     * Ask for all entities of type substation, verify that the returned list matches the seed data
     *
     * */
    @Test
    public void getSubstationEntities() throws ReefServiceException
    {
        EntityService es = helpers;
        List<Entity> list = es.getAllEntitiesWithType( "EquipmentGroup" );
        assertEquals( 2, list.size() );
        for ( Entity e : list )
        {
            assertTrue( e.getTypesList().contains( "EquipmentGroup" ) );
        }
    }

    /**
     * Ask for all entities of type su point and bkr
     *
     * */
    @Test
    public void getStatusOfTypeBreaker() throws ReefServiceException
    {
        EntityService es = helpers;

        List<Entity> list = es.getAllEntitiesWithTypes( Arrays.asList( "bkrStatus" ) );
        assertEquals( 2, list.size() );

        for ( Entity e : list )
        {
            assertTrue( e.getTypesList().contains( "bkrStatus" ) );
        }
    }

    /**
     * Ask for all entities of type su Setpoint
     *
     * */
    @Test
    public void getCommandOfTypeSetpoint() throws ReefServiceException
    {
        EntityService es = helpers;

        List<Entity> list = es.getAllEntitiesWithTypes( Arrays.asList( "Setpoint" ) );
        assertEquals( 2, list.size() );

        for ( Entity e : list )
        {
            assertTrue( e.getTypesList().contains( "Setpoint" ) );
            assertTrue( e.getTypesList().contains( "Command" ) );
        }
    }

    /**
     * Test that their is self-consistency between points and point entities
     */
    @Test
    public void pointToPointEntityConsistency() throws ReefServiceException
    {
        PointService ps = helpers;
        EntityService es = helpers;

        List<Point> points = ps.getAllPoints();
        List<Entity> point_entities = es.getAllEntitiesWithType( "Point" );

        assertEquals( points.size(), point_entities.size() ); // check that they have the same size
        Map<String, Point> pMap = getPointMap( points );
        Map<String, Entity> eMap = getEntityMap( point_entities );

        // check that the maps have an equivalent size with the lists
        // this assures that the points have no duplicate names
        assertEquals( points.size(), pMap.size() );
        assertEquals( pMap.size(), eMap.size() );

        assertEquals( pMap.keySet(), eMap.keySet() );
    }

    /**
     * Test that their is self-consistency between points, point entities, and equipment
     */
    @Test
    public void equipmentToPointConsistency() throws ReefServiceException
    {
        PointService ps = helpers;
        EntityService es = helpers;

        List<Point> points = ps.getAllPoints();
        List<Entity> point_entities = es.getAllEntitiesWithType( "Point" );

        assertEquals( points.size(), point_entities.size() ); // check that they have the same size
        Map<String, Point> pMap = getPointMap( points );
        Map<String, Entity> eMap = getEntityMap( point_entities );

        // check that the maps have an equivalent size with the lists
        // this assures that the points have no duplicate names
        assertEquals( points.size(), pMap.size() );
        assertEquals( pMap.size(), eMap.size() );

        assertEquals( pMap.keySet(), eMap.keySet() );
    }

    /**
     * Tes and only one feedback point
     */
    @Test
    public void allControlsHaveOnePointForFeedback() throws ReefServiceException
    {

        // get all commands in the system and fill out any relationships of type feedback with
        // points
        Entity request =
            Entity.newBuilder().addTypes( "Command" ).addRelations(
                    Relationship.newBuilder().setRelationship( "feedback" ).setDescendantOf( false ).addEntities(
                            Entity.newBuilder().addTypes( "Point" ) ) ).build();

        EntityService es = helpers;

        List<Entity> result = es.getEntities( request );

        for ( Entity e : result )
        {
            assertTrue( e.getTypesList().contains( "Command" ) );
            assertEquals( e.getRelationsCount(), 1 );
            Relationship r = e.getRelations( 0 );
            assertEquals( "feedback", r.getRelationship() );
            assertFalse( r.getDescendantOf() );
            assertEquals( 1, r.getEntitiesCount() );
            assertTrue( r.getEntities( 0 ).getTypesList().contains( "Point" ) );

        }
    }


    /**
     * Find all the points under a substation and their associated commands in one step.
     */
    @Test
    public void commandsToPointsMapping() throws ReefServiceException
    {
        EntityService es = helpers;
        // First get a substation we can use as an example root
        Entity sub = es.getEntityByName( "StaticSubstation" );

        // Tree request, asks for points under this substation and the commands associated with those points.
        Entity request =
            Entity.newBuilder().setUuid( sub.getUuid() ).addRelations(
                    Relationship.newBuilder().setRelationship( "owns" ).setDescendantOf( true ).addEntities(
                            Entity.newBuilder().addTypes( "Point" ).addRelations(
                                    Relationship.newBuilder().setRelationship( "feedback" ).setDescendantOf( true ).addEntities(
                                            Entity.newBuilder().addTypes( "Command" ) ) ) ) ).build();

        // Request will return the substation as a root node, with the relationship tree below it
        Entity tree = es.getEntityTree( request );

        // Only owns should be there
        assertEquals( tree.getRelationsCount(), 1 );

        // One step down we have all the points
        List<Entity> points = tree.getRelationsList().get( 0 ).getEntitiesList();

        // There are three points for thsi substation in the integration test configuration
        assertEquals( points.size(), 3 );

        // Keep a tally of how many command linkages we found
        int cmdCount = 0;

        // Go through the point entities, looking for associated commands
        for ( Entity point : points )
        {

            // Points without commands have no relationships populated
            if ( point.getRelationsCount() > 0 )
            {

                // Points with commands have only the "feedback" relationship
                assertEquals( point.getRelationsCount(), 1 );

                Relationship feedback = point.getRelationsList().get( 0 );
                assertEquals( feedback.getRelationship(), "feedback" );

                // Any feedback relationships should be populated
                List<Entity> commands = feedback.getEntitiesList();
                assertTrue( commands.size() > 0 );

                // We count the number of commands we have for testing purposes; we could create a map
                for ( Entity command : commands )
                {
                    assertTrue( command.getTypesList().contains( "Command" ) );
                    cmdCount++;
                }
            }
        }
        assertEquals( cmdCount, 3 );

    }

    /**
     * Select a random substation and look for presence of some well known equipment types.
     * 
     */
    @Test
    public void getEquipmentInASubstation() throws ReefServiceException
    {
        EntityService es = helpers;

        Entity substation = es.getAllEntitiesWithType( "EquipmentGroup" ).get( 0 );

        List<Entity> entities = es.getEntityRelatedChildrenOfType( substation.getUuid(), "owns", "Equipment" );

        assertNotSame( 0, entities.size() );
        Set<String> equipTypes = new HashSet<String>();

        for ( Entity e : entities )
        {
            for ( String type : e.getTypesList() )
            {
                equipTypes.add( type );
            }
        }

        // the canonical model has these equipment types in each substation
        assertTrue( equipTypes.contains( "Breaker" ) );
        assertTrue( equipTypes.contains( "Line" ) );

    }

}
