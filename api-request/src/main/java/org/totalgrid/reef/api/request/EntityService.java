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

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.proto.Model.Entity;
import org.totalgrid.reef.proto.Model.EntityAttributes;
import org.totalgrid.reef.proto.Model.ReefUUID;

import java.util.List;

/**
 * The EntityService provides a view into the "model" of the system. The model can be thought of as a large pool of
 * "entities" that represent objects in the system and edges (relationships) between those entities. There are many types of
 * relationships that can be modeled between entities and which relationships that are modeled in any particular instance
 * of a reef installation depend on what the system is being used for. The EntityService is primarily useful for
 * getting the "shape" of the system. Many of the entities in the "entity pool" also have a "concrete" representation that
 * includes type specific data. An example is Point, there is both an Entity representation of the point that is used to
 * describe logical relationships to equipment and commands and also a "concrete" representation available through the
 * PointService that includes added data like whether that point is currently abnormal etc. Clients are expected to use
 * the EntityService to query for the "shape" of the data and then take those entities, look for "concrete" types on the
 * returned entities and use concrete services for further interaction.
 * <p/>
 * Examples of relationship types (colors)  are*  - "owns"used  in power systems to model how points and commands are logically considered to be parts of equipment
 * - "feedback"denotes  which Points are affected by which Commands
 * - "source"denotes  the data provider for a Point or Command (communication pathway)
 * <p/>
 * The edges of one color (all Ex "owns" edges) will form a tree (no cyclic dependencies). Entities can be included in
 * many trees so may have many edges with different colors. (a Ex Point can be logically "owned" by a piece of
 * equipment and will also have a data "source" edge to the communication device that is measuring that value) Edges are
 * transitive so if A -> "owns" -> B and B -> "owns" -> C therefore A -> "owns" -> C. To differentiate between a "direct"
 * edge and a transitive edge we use the concept of "distance". The distance from A -> B and B -> C would be 1, between
 * A -> C would be 2. This distance can be explicitly specified to constrain queries. All edges have a direction, so in
 * the previous example it would be said "A owns B" and "B is owned by A". The "descendant_of" field in the proto can be
 * used to set this direction, true means get children ("A owns B"), false means get parents ("B is owned by A").
 * <p/>
 * In each installation of Reef there are a further set of constraints applied over this basic model that make the model
 * easier to consume and reason about, there should be accompanying documentation that describe what those constraints
 * are. In a future release those constraints will themselves be queryable so applications can be more self configuring,
 * currently the developer needs to have a decent idea as to the model to use the construct a useful query.
 */
public interface EntityService {

    /**
     * Get all entities, should not be used in large systems
     *
     * @return all entities in the system
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    List<Entity> getAllEntities() throws ReefServiceException;

    /**
     * Get an entity using its unique identification.
     *
     * @param uid The entity id.
     * @return The entity object.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    Entity getEntityByUid(ReefUUID uid) throws ReefServiceException;

    /**
     * Get an entity using its name.
     *
     * @param name The configured name of the entity.
     * @return The entity object.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    Entity getEntityByName(String name) throws ReefServiceException;

    /**
     * Find all entities with a specified type.
     *
     * @param typ The entity type to search for.
     * @return The list of entities that have the specified type.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    List<Entity> getAllEntitiesWithType(String typ) throws ReefServiceException;

    /**
     * Find all entities with a specified type.
     *
     * @param types List of required entity types to search for.
     * @return The list of entities that have the specified type.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    List<Entity> getAllEntitiesWithTypes(List<String> types) throws ReefServiceException;

    /**
     * Return all child entities that have the correct type and a matching relationship to the parent Entity. The results
     * are "flattened" and all children are returned in one list so any relationships or groupings of the child entities
     * will be discarded.
     *
     * @param parent       a reference to the parent entity on which to root the request
     * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
     * @param typ          the "type" or "class" the matching children need to have
     * @return list of all children in arbitrary order
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    List<Entity> getEntityRelatedChildrenOfType(ReefUUID parent, String relationship, String typ) throws ReefServiceException;

    /**
     * Use this function to make a complex entity model query. It is usually possible to satisfy most entity requirements
     * with a single call to the Entity service. This is accomplished by building a request entity that has the same
     * tree "shape" as the result you want to display. The entity service will then "fill in" that tree with the matching
     * entities. A tree query assumes you have a single parent node that you are basing the request of, use getEntities
     * if doing a more general query that may return more than one tree (no root node)
     * <p/>
     * Entity TODO will become EntitySelector in the future
     *
     * @param an Entity describing the tree request
     * @return a "filled out" copy of the original tree request.
     */
    Entity getEntityTree(Entity entityTree) throws ReefServiceException;

    /**
     * Use this function to make a complex entity model query. This query is very similar to getEntityTree but doesn't
     * assume a "root node" and can therefore be used to make any request including a EntityTree query
     * <p/>
     * Entity TODO will become EntitySelector in the future
     *
     * @param entityTree an Entity describing the tree request
     * @return a list of Entities that matched query possibly with filled in relationships
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    List<Entity> getEntities(Entity entityTree) throws ReefServiceException;

    /**
     * Get the attributes for a specified Entity.
     *
     * @param uid The entity id.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes getEntityAttributes(ReefUUID uid) throws ReefServiceException;

    /**
     * Remove a specific attribute by name for a particular Entity.
     *
     * @param uid      The entity id.
     * @param attrName The name of the attribute.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes removeEntityAttribute(ReefUUID uid, String attrName) throws ReefServiceException;

    /**
     * Clear all attributes for a specified Entity.
     *
     * @param uid The entity id.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes clearEntityAttributes(ReefUUID uid) throws ReefServiceException;

    /**
     * Set a boolean attribute by name for a specified Entity.
     *
     * @param uid   The entity id.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute(ReefUUID uid, String name, boolean value) throws ReefServiceException;

    /**
     * Set a signed 64-bit integer attribute by name for a specified Entity.
     *
     * @param uid   The entity id.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute(ReefUUID uid, String name, long value) throws ReefServiceException;

    /**
     * Set an attribute of type double by name for a specified Entity.
     *
     * @param uid   The entity id.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute(ReefUUID uid, String name, double value) throws ReefServiceException;

    /**
     * Set a string attribute by name for a specified Entity.
     *
     * @param uid   The entity id.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute(ReefUUID uid, String name, String value) throws ReefServiceException;

    /**
     * Set an attribute of type Array<Byte> by name for a specified Entity.
     *
     * @param uid   The entity id.
     * @param name  The name of the attribute.
     * @param value The attribute value.
     * @return The entity and its associated attributes.
     * @throws org.totalgrid.reef.japi.ReefServiceException
     */
    EntityAttributes setEntityAttribute(ReefUUID uid, String name, byte[] value) throws ReefServiceException;
}