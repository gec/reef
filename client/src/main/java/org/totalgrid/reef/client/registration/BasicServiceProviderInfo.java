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
package org.totalgrid.reef.client.registration;

import org.totalgrid.reef.client.ServiceProviderFactory;
import org.totalgrid.reef.client.ServiceProviderInfo;
import org.totalgrid.reef.client.internal.ProviderFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class to build instances of ServiceProviderInfo
 */
public class BasicServiceProviderInfo implements ServiceProviderInfo
{
    private final ServiceProviderFactory factory;
    private final List<Class<?>> klasses;

    /**
     * @param factory Factory to build client implementations
     * @param klasses List of classes implemented by the client implementation
     */
    public BasicServiceProviderInfo( ServiceProviderFactory factory, Class<?>... klasses )
    {
        this.factory = factory;
        this.klasses = Arrays.asList( klasses );
    }

    @Override
    public ProviderFactory getFactory()
    {
        return factory;
    }

    @Override
    public List<Class<?>> getInterfacesImplemented()
    {
        return klasses;
    }
}
