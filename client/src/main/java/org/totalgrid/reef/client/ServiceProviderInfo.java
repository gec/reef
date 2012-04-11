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

import org.totalgrid.reef.client.internal.ProviderFactory;

import java.util.List;

/**
 * Each service-client API consists of an interface and an implementation. In many cases,
 * including the default service-client APIs, a single class actually implements
 * many of the service APIs.
 */
public interface ServiceProviderInfo
{
    /**
     * @return factory class that provides the implementation of the listed interfaces
     */
    ProviderFactory getFactory();

    /**
     * @return list of all interfaces implemented by this provider.
     */
    List<Class<?>> getInterfacesImplemented();
}
