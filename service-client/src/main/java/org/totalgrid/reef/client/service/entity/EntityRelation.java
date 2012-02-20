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
package org.totalgrid.reef.client.service.entity;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This structure is used to construct more complex queries than we expose through the regular APIs.
 *
 * Most of the queries in the Entity service could be re-expressed using EntityRelation structures:
 * <pre>
 * getEntityRelatedChildrenOfType =>
 *  getEntityRelations(parent, new EntityRelation(relationship, typeName, true))
 *
 * getEntityImmediateChildren =>
 *  getEntityRelations(parent, new EntityRelation(relationship, true))
 *
 * getEntityImmediateParents =>
 *  getEntityRelations(parent, new EntityRelation(relationship, false))
 *
 * More complex queries are also possible by chaining these relationship together.
 *
 * "get equipment groups and their equipment" =>
 *   List<EntityRelation> rels = new LinkedList<EntityRelation>();
 *   rels.add(new EntityRelation("owns", "EquipmentGroup", true));
 *   rels.add(new EntityRelation("owns", "Equipment", true));
 *   getEntityRelations(parent, rels);
 *
 * "get commands and their endpoints" =>
 *   List<EntityRelation> rels = new LinkedList<EntityRelation>();
 *   rels.add(new EntityRelation("owns", "Command", true));
 *   rels.add(new EntityRelation("source", "CommunicationEndpoint", false));
 *   getEntityRelations(parent, rels);
 * </pre>
 */
public class EntityRelation
{

    private final String relationship;
    private final List<String> types;
    private final boolean child;
    private final int depth;

    /**
     * Describes the entities we are looking for.
     * @param relationship relationship type ("owns", "source", "feedback")
     * @param types list of types the related entity can have. An empty or null list indicates we dont care about type
     * @param child whether we are looking for children (true) or parents (false)
     * @param depth how deep we want to search, valid values are >= 1 or -1 if we don't care about depth
     */
    public EntityRelation( String relationship, List<String> types, boolean child, int depth )
    {
        this.relationship = relationship;
        this.types = types;
        this.child = child;
        this.depth = depth;
    }

    /**
     * Equivalent to EntityRelation(relationship,types,child,-1)
     */
    public EntityRelation( String relationship, List<String> types, boolean child )
    {
        this.relationship = relationship;
        this.types = types;
        this.child = child;
        this.depth = -1;
    }

    /**
     * Equivalent to EntityRelation(relationship,type,child,-1)
     */
    public EntityRelation( String relationship, String type, boolean child )
    {
        this.relationship = relationship;
        this.types = new LinkedList<String>();
        this.types.add( type );
        this.child = child;
        this.depth = -1;
    }

    /**
     * Equivalent to EntityRelation(relationship,new List(type),child,-1)
     */
    public EntityRelation( String relationship, String type, boolean child, int depth )
    {
        this.relationship = relationship;
        this.types = new LinkedList<String>();
        this.types.add( type );
        this.child = child;
        this.depth = depth;
    }

    /**
     * Equivalent to EntityRelation(relationship,null,child,-1)
     */
    public EntityRelation( String relationship, boolean child )
    {
        this.relationship = relationship;
        this.types = null;
        this.child = child;
        this.depth = -1;
    }

    /**
     * Equivalent to EntityRelation(relationship,null,child,depth)
     */
    public EntityRelation( String relationship, boolean child, int depth )
    {
        this.relationship = relationship;
        this.types = null;
        this.child = child;
        this.depth = depth;
    }

    public boolean getChild()
    {
        return child;
    }

    public int getDepth()
    {
        return depth;
    }

    public String getRelationship()
    {
        return relationship;
    }

    public List<String> getTypes()
    {
        return types;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( relationship ).append( ":" );
        if ( depth != -1 )
            sb.append( depth );
        else
            sb.append( "*" );
        sb.append( ":" ).append( child ).append( ":" );
        if ( types == null || types.isEmpty() )
            sb.append( "*" );
        else
        {
            String sep = "";
            for ( String typ : types )
            {
                sb.append( sep );
                sb.append( typ );
                sep = ",";
            }
        }

        return sb.toString();
    }

    public static EntityRelation fromString( String str )
    {
        try
        {
            String[] arr = str.split( ":" );
            if ( arr.length != 4 )
                throw new IllegalArgumentException( "EntityRelation string should be 4 parts separated by :" );
            String relationship = arr[0];
            Boolean child = Boolean.parseBoolean( arr[2] );
            int depth;
            if ( "*".compareTo( arr[1] ) == 0 )
                depth = -1;
            else
                depth = Integer.parseInt( arr[1] );
            String[] types = arr[3].split( "," );

            return new EntityRelation( relationship, Arrays.asList( types ), child, depth );
        }
        catch ( NumberFormatException nfe )
        {
            throw new IllegalArgumentException( "Cannot parse EntityRelation string" + str + " err: " + nfe.getMessage() );
        }
    }
}
