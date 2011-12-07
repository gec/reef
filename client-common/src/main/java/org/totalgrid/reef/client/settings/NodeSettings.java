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
package org.totalgrid.reef.client.settings;

import org.totalgrid.reef.client.settings.util.PropertyLoading;
import org.totalgrid.reef.client.settings.util.PropertyReader;

import java.io.IOException;
import java.util.Dictionary;

/**
 * Encapsulates the settings for an individual application node. NodeSettings are used to inform the reef
 * services where this application is running and what network it is connected to.
 *
 * Each application should use a unique node name if there is any chance that more than one copy of the
 * application may be used at a time.
 */
public class NodeSettings
{
    private String defaultNodeName;
    private String location;

    // TODO: make network a list - backlog-65
    private String network;

    /**
     * Manual nodeSettings constructor
     * @param defaultNodeName name of the application (usually prepended onto capabilities)
     * @param location "physical" location of the application, what computer its running on usually
     * @param network "name" of the network that is accessible to the application
     */
    public NodeSettings( String defaultNodeName, String location, String network )
    {
        this.defaultNodeName = defaultNodeName;
        this.location = location;
        this.network = network;
    }

    /**
     * constructs a NodeSettings from the passed in dictionary.
     * @param properties dictionary object, usually a java.util.Properties object
     * @throws IllegalArgumentException if any of the required fields are missing from the dictionary
     */
    public NodeSettings( Dictionary properties ) throws IllegalArgumentException
    {
        defaultNodeName = PropertyLoading.getString( "org.totalgrid.reef.node.name", properties );
        location = PropertyLoading.getString( "org.totalgrid.reef.node.location", properties );
        network = PropertyLoading.getString( "org.totalgrid.reef.node.network", properties );
    }

    /**
     * Load from a cfg text file.
     * @param fileName relative or absolute file path
     * @throws IllegalArgumentException if any of the needed fields are mssing
     * @throws IOException if the file is inaccessible
     */
    public NodeSettings( String fileName ) throws IllegalArgumentException, IOException
    {
        this( PropertyReader.readFromFile( fileName ) );
    }

    /**
     * @return baseName for the application
     */
    public String getDefaultNodeName()
    {
        return defaultNodeName;
    }

    /**
     * @return  "physical" location of the application, what computer its running on usually
     */
    public String getLocation()
    {
        return location;
    }

    /**
     * Returns a user assigned name for the network. In a distributed system it is often very
     * difficult to determine what addresses or networks are accessible to an application. We use
     * a network name to indicate what network the application is on and therefore what it has
     * access to.
     *
     * @return "name" of the network that is accessible to the application
     */
    public String getNetwork()
    {
        return network;
    }
}
