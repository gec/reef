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
package org.totalgrid.reef.client.service.list;

import org.totalgrid.reef.client.ServiceProviderInfo;
import org.totalgrid.reef.client.ServicesList;
import org.totalgrid.reef.client.types.ServiceTypeInformation;

import java.util.LinkedList;
import java.util.List;


/**
 * servicesList implementation for the apis in ReefServices
 */
public class ReefServices implements ServicesList
{
    /*
      Note that this class is only implemented in java so it shows up in javadoc.
     */

    public List<ServiceTypeInformation<?, ?>> getServiceTypeInformation()
    {
        return ReefServicesList.getServicesList();
    }

    public List<ServiceProviderInfo> getServiceProviders()
    {
        List<ServiceProviderInfo> list = new LinkedList<ServiceProviderInfo>();

        // having package imports where the javadoc tool can see them causes the api-enhancer
        // step to fail on unknown imports.

        list.add( org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImplServiceList.getServiceInfo() );
        list.add( org.totalgrid.reef.client.service.impl.AllScadaServiceJavaShimServiceList.getServiceInfo() );
        list.add( org.totalgrid.reef.client.service.impl.async.AllScadaServiceAsyncJavaShimServiceList.getServiceInfo() );
        list.add( org.totalgrid.reef.client.sapi.rpc.impl.ClientOperationsServiceProviders.getScalaServiceInfo() );
        list.add( org.totalgrid.reef.client.sapi.rpc.impl.ClientOperationsServiceProviders.getJavaServiceInfo() );
        list.add( org.totalgrid.reef.client.sapi.rpc.impl.ClientOperationsServiceProviders.getJavaAsyncServiceInfo() );
        return list;
    }
}
