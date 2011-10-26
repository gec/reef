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

import java.util.List;

import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.proto.Model.Entity;
import org.totalgrid.reef.proto.Model.EntityAttributes;
import org.totalgrid.reef.proto.Model.ReefUUID;

/**
 * <p>
 *   Service for retrieving entity relationships and storing/retrieving entity attributes.</p>
 *
 * <p>
 *   Note: Many terms for this section are take from <a href="http://en.wikipedia.org/wiki/Graph_theory">graph theory</a>.</p>
 *
 * <p>
 *   The EntityService provides access to the system model. The model is both the pool of
 *   "entities" and the relationships (edges) connecting those entities to each other.
 *   Entity types include: Agent, Substations, EquipmentGroup, Breaker, Point, etc.
 *   Two entities may be related by more than one type of relationship (examples: owns,
 *   feedback, etc.)</p>
 *
 * <p>
 *   The specific entities and relationships modeled in a particular reef installation
 *   depends on what the system is being used for (ex: SCADA, Hydro, Microgrid, etc.).</p>
 *
 * <p>
 *   Many of the entities in the "entity pool" have more specific type information beyond Entity.
 *   An example is Point. There is an Entity representation of a point that is used to
 *   describe logical relationships to equipment and commands. At the same time, there is a Point
 *   representation available through the PointService that includes added data like whether that
 *   point is currently abnormal etc. Clients are expected to use the more specific services for
 *   basic relationship queries and to retrieve the detailed information available for each specific type. The
 *   EntityService is available for more complex queries that select objects based on the the many
 *   relationships between entities. Once these entities are returned, the client can use the
 *   type specific services to get more type-specific information.</p>
 *
 * <p>
 *   Examples of relationship types (colors)  are:</p>
 *   <ul>
 *     <li>owns - used in power systems to model how points and commands are logically considered to be parts of equipment</li>
 *     <li>feedback - denotes which Points are affected by which Commands</li>
 *     <li>source - denotes the data provider for a Point or Command (communication pathway)</li>
 *   </ul>
 *
 * <p>
 * In each installation of Reef there are a further set of constraints applied over this basic model that make the model
 * easier to consume and reason about, there should be accompanying documentation that describe what those constraints
 * are. In a future release those constraints will themselves be queryable so applications can be more self configuring,
 * Currently the developer needs to have a decent idea as to the model to construct a useful query.</p>
 */
public interface EntityService
{

    /**
     * Get all entities, should not be used in large systems
     *
     * @return all entities in the system
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    List<Entity> getAllEntities() throws ReefServiceException;

    /**
     * Get an entity using its unique identification.
     *
     * @param uid The entity id.
     * @return The entity object.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    Entity getEntityByUid( ReefUUID uid ) throws ReefServiceException;

    /**
     * Get an entity using its name.
     *
     * @param name The configured name of the entity.
     * @return The entity object.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    Entity getEntityByName( String name ) throws ReefServiceException;

    /**
     * Find an entity using its name, returns null if not found
     *
     * @param name The configured name of the entity.
     * @return The entity object or null
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    Entity findEntityByName( String name ) throws ReefServiceException;

    /**
     * Find all entities with a specified type.
     *
     * @param typeName The entity type to search for.
     * @return The list of entities that have the specified type.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    List<Entity> getAllEntitiesWithType( String typeName ) throws ReefServiceException;

    /**
     * Find all entities matching at least one of the specified types.
     *
     * @param types List of entity types to search for. An entity matches if it has at least one of the specified types.
     * @return The list of entities that have the specified types.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    List<Entity> getAllEntitiesWithTypes( List<String> types ) throws ReefServiceException;

    /**
     * Return all child entities that have the correct type and a matching
     * relationship to the specified parent Entity. The results
     * are "flattened" and all children are returned in one list so any
     * relationships or groupings of the child entities will be discarded.
     *
     * @param parent       a reference to the parent entity on which to root the request
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @param typeName     the "type" or "class" the matching children need to have
     * @return list of all children in arbitrary order
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    List<Entity> getEntityRelatedChildrenOfType( ReefUUID parent, String relationship, String typeName ) throws ReefServiceException;

    /**
     * Return direct children of the parent Entity (distance of 1). Just children are returned.
     * @param parent       a reference to the parent entity on which to root the request
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @return  list of all children in arbitrary order
     * @throws ReefServiceException
     */
    List<Entity> getEntityImmediateChildren( ReefUUID parent, String relationship ) throws ReefServiceException;

    /**
     * Return direct children of the parent Entity (distance of 1). Just children are returned.
     * @param parent       a reference to the parent entity on which to root the request
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @param constrainingTypes list of children types we would like to returned, only those children that have atleast one
     *                          of the indicated types are returned
     * @return  list of all children in arbitrary order
     * @throws ReefServiceException
     */
    List<Entity> getEntityImmediateChildren( ReefUUID parent, String relationship, List<String> constrainingTypes ) throws ReefServiceException;

    /**
     * Return a tree of upto depth with all nodes related to each other
     * @param parent       a reference to the parent entity on which to root the request
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @param depth        how many plies deep we want to
     * @return  the root entity filled out with children
     * @throws ReefServiceException
     */
    Entity getEntityChildren( ReefUUID parent, String relationship, int depth ) throws ReefServiceException;

    /**
     * Return a tree of upto depth with all nodes in constraining types related to each other
     * @param parent       a reference to the parent entity on which to root the request
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @param depth        how many plies deep we want to
     * @param constrainingTypes list of children types we would like to returned, only those children that have atleast one
     *                          of the indicated types are returned
     * @return  the root entity filled out with children
     * @throws ReefServiceException
     */
    Entity getEntityChildren( ReefUUID parent, String relationship, int depth, List<String> constrainingTypes ) throws ReefServiceException;

    /**
     * Return a tree of upto depth with all nodes in constraining types related to each other
     * @param parentType   a type for all of the roots we want to use ("Root")
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @param depth        how many plies deep we want to
     * @param constrainingTypes list of children types we would like to returned, only those children that have atleast one
     *                          of the indicated types are returned
     * @return  the root entity filled out with children
     * @throws ReefServiceException
     */
    List<Entity> getEntityChildrenFromTypeRoots( String parentType, String relationship, int depth, List<String> constrainingTypes )
        throws ReefServiceException;

    /**
     * Return a tree of entities based on a complex entity model query. It is usually possible to satisfy most entity requirements
     * with a single call to the Entity service. This is accomplished by building a request entity that has the same
     * tree "shape" as the result you want to display. The entity service will then "fill in" that tree with the matching
     * entities. A tree query assumes you have a single parent node that you are basing the request on. Use getEntities
     * if doing a more general query that may return more than one tree (i.e. no root node).
     * <p/>
     *
     * <p>
     *   The edges of one color (ex: all "owns" edges) will form a tree (no cyclic dependencies).
     *   Entities can be included in many trees so may have many edges with different colors.
     *   For example, Point can be logically "owned" by a piece of equipment and will also have a data
     *   "source" edge to the communication device that is measuring that value. Edges are transitive
     *   so if A -> "owns" -> B and B -> "owns" -> C therefore A -> "owns" -> C. To differentiate
     *   between a "direct" edge and a transitive edge we use the concept of "distance". The distance
     *   from A -> B and B -> C would be 1, between A -> C would be 2. This distance can be explicitly
     *   specified to constrain queries. All edges have a direction, so in the previous example it would
     *   be said "A owns B" and "B is owned by A". The "descendant_of" field in the proto can be
     * used to set this direction, true means get children ("A owns B"), false means get parents ("B is owned by A").</p>
     *
     * @param entityTree Entity describing the tree request
     * @return a "filled out" copy of the original tree request.
     */
    Entity getEntityTree( Entity entityTree ) throws ReefServiceException;

    /**
     * Return a list of entities based on a complex entity model query.
     * This query is very similar to getEntityTree but doesn't assume a
     * "root node" and can therefore be used to make any request including a EntityTree query
     * <p/>
     *
     * @param entityTree Entity describing the tree request
     * @return a list of Entities that matched query possibly with filled in relationships
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    List<Entity> getEntities( Entity entityTree ) throws ReefServiceException;

    /**
     * Get all attributes associated with a specified Entity.
     *
     * @param uid The entity uid.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes getEntityAttributes( ReefUUID uid ) throws ReefServiceException;

    /**
     * Remove a specific attribute by name for a specified Entity.
     *
     * @param uid      The entity uid.
     * @param attrName The name of the attribute.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes removeEntityAttribute( ReefUUID uid, String attrName ) throws ReefServiceException;

    /**
     * Clear all attributes for a specified Entity.
     *
     * @param uid The entity uid.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes clearEntityAttributes( ReefUUID uid ) throws ReefServiceException;

    /**
     * Set a boolean attribute by name for a specified Entity.
     *
     * @param uid   The entity uid.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute( ReefUUID uid, String name, boolean value ) throws ReefServiceException;

    /**
     * Set a signed 64-bit integer attribute by name for a specified Entity.
     *
     * @param uid   The entity uid.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute( ReefUUID uid, String name, long value ) throws ReefServiceException;

    /**
     * Set a double attribute by name for a specified Entity.
     *
     * @param uid   The entity uid.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute( ReefUUID uid, String name, double value ) throws ReefServiceException;

    /**
     * Set a string attribute by name for a specified Entity.
     *
     * @param uid   The entity uid.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute( ReefUUID uid, String name, String value ) throws ReefServiceException;

    /**
     * Set an Array<Byte> attribute by name for a specified Entity.
     *
     * @param uid   The entity uid.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.api.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute( ReefUUID uid, String name, byte[] value ) throws ReefServiceException;
}