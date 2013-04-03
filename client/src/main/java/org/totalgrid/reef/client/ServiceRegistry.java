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

import org.totalgrid.reef.client.types.ServiceTypeInformation;

/**
 * the service registry keeps the low-level details of routing and type information for the types that are sent
 * over the wire to the server. It is also where we keep the factories for creating the "semantic" high level
 * api classes (PointService, EntityService etc.).
 */
public interface ServiceRegistry
{
    void addServicesList( ServicesList servicesList );

    /**
     * add a factory for creating a high level api class
     * @param info describes the interfaces implemented and the implementing class
     */
    void addServiceProvider( ServiceProviderInfo info );

    /**
     * add the low-level information on a type we plan on sending over the wire
     */
    <T, U> void addServiceTypeInformation( ServiceTypeInformation<T, U> typeInformation );

    /**
     * gets the ServiceTypeInformation for an objects class.
     */
    <T> ServiceTypeInformation<T, ?> getServiceTypeInformation( Class<T> klass );
}
