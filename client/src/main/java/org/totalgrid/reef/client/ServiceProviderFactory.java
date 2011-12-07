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
 * ServiceProviderFactories are created by "service-client" packages to implement one or more
 * service interfaces.
 *
 * Currently these service providers are only creatable in scala because they use the scala
 * implementation of Client that has more low-level functions.
 */
public interface ServiceProviderFactory
{
    /**
     * Currently we are passing the scala client to the rpc factoryies and casting to
     * break the circular dependency between the scala and java clients.
     *
     * Once the java Client interface has all of the functions on the scala client we
     * can fix this interface
     */
    Object createRpcProvider( Object client );
}
