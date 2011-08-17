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
package org.totalgrid.reef.japi.client;

import org.totalgrid.reef.japi.client.util.PropertyLoading;

import java.util.Dictionary;
import java.util.Properties;

public class NodeSettings
{

    private String defaultNodeName;

    private String location;

    // TODO: make network a list
    private String network;

    public NodeSettings( String defaultNodeName, String location, String network )
    {
        this.defaultNodeName = defaultNodeName;
        this.location = location;
        this.network = network;
    }

    public NodeSettings( Dictionary properties )
    {
        this.defaultNodeName = PropertyLoading.getString( "org.totalgrid.reef.node.name", properties );
        this.location = PropertyLoading.getString( "org.totalgrid.reef.node.location", properties );
        this.network = PropertyLoading.getString( "org.totalgrid.reef.node.network", properties );
    }

    public String getDefaultNodeName()
    {
        return defaultNodeName;
    }

    public String getLocation()
    {
        return location;
    }

    public String getNetwork()
    {
        return network;
    }
}
