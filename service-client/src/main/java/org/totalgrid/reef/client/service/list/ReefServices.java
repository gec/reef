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
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImplServiceList;
import org.totalgrid.reef.client.service.impl.AllScadaServiceJavaShimServiceList;
import org.totalgrid.reef.client.types.ServiceTypeInformation;

import java.util.LinkedList;
import java.util.List;


/**
 * servicesList implementation for the apis in ReefServices
 */
public class ReefServices implements ServicesList
{
    public List<ServiceTypeInformation<?, ?>> getServiceTypeInformation()
    {
        return ReefServicesList.getServicesList();
    }

    public List<ServiceProviderInfo> getServiceProviders()
    {
        List<ServiceProviderInfo> list = new LinkedList<ServiceProviderInfo>();
        list.add( AllScadaServiceImplServiceList.getServiceInfo() );
        list.add( AllScadaServiceJavaShimServiceList.getServiceInfo() );
        return list;
    }
}
