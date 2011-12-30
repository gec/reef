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
package org.totalgrid.reef.integration.helpers;

import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.SubscriptionCreationListener;

import java.util.LinkedList;
import java.util.List;


public class MockSubscriptionBindingListener implements SubscriptionCreationListener
{

    public List<SubscriptionBinding> bindings = new LinkedList<SubscriptionBinding>();

    public synchronized void onSubscriptionCreated( SubscriptionBinding binding )
    {
        bindings.add( binding );
    }

    public synchronized int size()
    {
        return bindings.size();
    }

    public synchronized void cancelAll()
    {
        for ( SubscriptionBinding binding : bindings )
        {
            binding.cancel();
        }
    }
}
