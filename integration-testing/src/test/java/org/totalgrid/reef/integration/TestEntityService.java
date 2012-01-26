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

import org.totalgrid.reef.client.service.CommandService;
import org.totalgrid.reef.client.service.entity.EntityRelation;
import org.totalgrid.reef.client.exception.BadRequestException;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.service.EntityService;
import org.totalgrid.reef.client.service.PointService;
import org.totalgrid.reef.client.service.proto.Model.*;

import java.util.*;

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
        List<Entity> list = es.getEntitiesWithType( "*" );
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
        List<Entity> list = es.getEntitiesWithType( "EquipmentGroup" );
        assertTrue( 2 <= list.size() );
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

        List<Entity> list = es.getEntitiesWithTypes( Arrays.asList( "bkrStatus" ) );
        assertTrue( 2 <= list.size() );

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

        List<Entity> list = es.getEntitiesWithTypes( Arrays.asList( "Setpoint" ) );
        assertTrue( 2 <= list.size() );

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

        List<Point> points = ps.getPoints();
        List<Entity> point_entities = es.getEntitiesWithType( "Point" );

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

        List<Point> points = ps.getPoints();
        List<Entity> point_entities = es.getEntitiesWithType( "Point" );

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

        List<EntityRelation> relations = new LinkedList<EntityRelation>();
        relations.add( new EntityRelation( "feedback", "Point", false ) );

        EntityService es = helpers;

        List<Entity> result = es.getEntityRelationsFromTypeRoots( "Command", relations );

        List<ReefUUID> commandRoots = new LinkedList<ReefUUID>();

        for ( Entity e : result )
        {
            assertTrue( e.getTypesList().contains( "Command" ) );
            assertEquals( e.getRelationsCount(), 1 );
            Relationship r = e.getRelations( 0 );
            assertEquals( "feedback", r.getRelationship() );
            assertFalse( r.getDescendantOf() );
            assertTrue( 1 >= r.getEntitiesCount() );
            assertTrue( r.getEntities( 0 ).getTypesList().contains( "Point" ) );
            commandRoots.add( e.getUuid() );
        }

        // verify that the EntityRelationsForParents call is equivalent to FromTypeRoots query
        List<Entity> byParents = es.getEntityRelationsForParents( commandRoots, relations );
        assertEquals( result, byParents );
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

        List<EntityRelation> relations = new LinkedList<EntityRelation>();
        relations.add( new EntityRelation( "owns", "Point", true ) ); // all point children
        relations.add( new EntityRelation( "feedback", "Command", true ) ); // feedback commands for those points

        // Request will return the points and their children beneath them
        List<Entity> points = es.getEntityRelations( sub.getUuid(), relations );

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
        assertEquals( cmdCount, 4 );

    }

    /**
     * Select a random substation and look for presence of some well known equipment types.
     * 
     */
    @Test
    public void getEquipmentInASubstation() throws ReefServiceException
    {
        EntityService es = helpers;

        Entity substation = es.getEntitiesWithType( "EquipmentGroup" ).get( 0 );

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

    @Test
    public void batchGetEntitiesAndPoints() throws ReefServiceException
    {
        EntityService es = helpers;

        List<String> names = new ArrayList<String>();
        List<ReefUUID> uuids = new ArrayList<ReefUUID>();

        List<Entity> pointEntities = es.getEntitiesWithType( "Point" );

        for ( Entity e : pointEntities )
        {
            names.add( e.getName() );
            uuids.add( e.getUuid() );
        }

        // test that batch queries will return same list as by type query
        List<Entity> batchRequestByName = es.getEntitiesByNames( names );
        assertArrayEquals( pointEntities.toArray(), batchRequestByName.toArray() );

        List<Entity> batchRequestByUuid = es.getEntitiesByUuids( uuids );
        assertArrayEquals( pointEntities.toArray(), batchRequestByUuid.toArray() );

        PointService ps = helpers;

        // test that this is the same list as getting all points (since we have entites of Point Type)
        List<Point> allPoints = ps.getPoints();

        assertArrayEquals( ps.getPointsByNames( names ).toArray(), allPoints.toArray() );
        assertArrayEquals( ps.getPointsByUuids( uuids ).toArray(), allPoints.toArray() );
    }

    @Test
    public void batchGetCommands() throws ReefServiceException
    {
        EntityService es = helpers;

        List<String> names = new ArrayList<String>();
        List<ReefUUID> uuids = new ArrayList<ReefUUID>();

        List<Entity> commandEntities = es.getEntitiesWithType( "Command" );

        for ( Entity e : commandEntities )
        {
            names.add( e.getName() );
            uuids.add( e.getUuid() );
        }

        CommandService cs = helpers;

        // test that this is the same list as getting all points (since we have entites of Point Type)
        List<Command> allCommands = cs.getCommands();

        assertArrayEquals( cs.getCommandsByNames( names ).toArray(), allCommands.toArray() );
        assertArrayEquals( cs.getCommandsByUuids( uuids ).toArray(), allCommands.toArray() );
    }

    @Test
    public void testEntityRelationCallsFailWithBadRequests() throws ReefServiceException
    {

        List<EntityRelation> relations = new LinkedList<EntityRelation>();
        EntityService es = helpers;

        try
        {
            List<Entity> result = es.getEntityRelationsFromTypeRoots( "Command", relations );
            assertEquals( "Should have thrown exception", "" );
        }
        catch ( BadRequestException e )
        {
            // expected failure
        }
        try
        {
            relations.add( new EntityRelation( "owns", true, 0 ) );
            List<Entity> result = es.getEntityRelationsFromTypeRoots( "Command", relations );
            assertEquals( "Should have thrown exception", "" );
        }
        catch ( BadRequestException e )
        {
            // expected failure
        }
        try
        {
            relations.clear();
            relations.add( new EntityRelation( "owns", true, -2 ) );
            List<Entity> result = es.getEntityRelationsFromTypeRoots( "Command", relations );
            assertEquals( "Should have thrown exception", "" );
        }
        catch ( BadRequestException e )
        {
            // expected failure
        }
    }
}
