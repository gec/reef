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
package org.totalgrid.reef.api.japi.settings.util;


import java.util.Dictionary;

public class PropertyLoading
{
    public static String getString( String id, Dictionary props, String onDefault ) throws IllegalArgumentException
    {
        String prop = (String)props.get( id );
        if ( prop == null )
            return onDefault;
        else
            return prop;
    }

    public static Boolean getBoolean( String id, Dictionary props, Boolean onDefault ) throws IllegalArgumentException
    {
        String prop = (String)props.get( id );
        if ( prop == null )
            return onDefault;
        else
            return Boolean.parseBoolean( prop );
    }

    public static String getString( String id, Dictionary props ) throws IllegalArgumentException
    {
        String prop = (String)props.get( id );
        if ( prop == null )
        {
            throw new IllegalArgumentException( "Could not load configuration. Missing: " + id );
        }
        return prop;
    }

    public static int getInt( String id, Dictionary props ) throws IllegalArgumentException
    {
        String prop = getString( id, props );
        return Integer.parseInt( prop );
    }

    public static boolean getBoolean( String id, Dictionary props ) throws IllegalArgumentException
    {
        String prop = getString( id, props );
        return Boolean.parseBoolean( prop );
    }
}
