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
package org.totalgrid.reef.client;

/**
 * If we need to address a particular node we can use an AddressableDestination constructed
 * with the address we want to receive the request.
 */
public class AddressableDestination implements Routable
{
    private String address;

    /**
     * @param address routing key for the request
     * @throws IllegalArgumentException if the address is blank or null
     */
    public AddressableDestination( String address )
    {
        if ( address == null || address.length() == 0 )
            throw new IllegalArgumentException( "Cannot use null or blank destination" );
        this.address = address;
    }

    public String getKey()
    {
        return address;
    }

    @Override
    public String toString()
    {
        return "RoutingKey: " + address;
    }
}
