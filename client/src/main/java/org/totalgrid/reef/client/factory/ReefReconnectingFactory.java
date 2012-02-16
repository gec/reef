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
package org.totalgrid.reef.client.factory;

import net.agileautomata.executor4s.ExecutorService;
import net.agileautomata.executor4s.Executors;
import net.agileautomata.executor4s.Minutes;
import org.totalgrid.reef.broker.BrokerConnection;
import org.totalgrid.reef.broker.BrokerConnectionFactory;
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory;
import org.totalgrid.reef.client.Connection;
import org.totalgrid.reef.client.ConnectionWatcher;
import org.totalgrid.reef.client.ReconnectingConnectionFactory;
import org.totalgrid.reef.client.ServicesList;
import org.totalgrid.reef.client.javaimpl.ConnectionWrapper;
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection;
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultReconnectingFactory;
import org.totalgrid.reef.client.settings.AmqpSettings;

import java.util.HashSet;
import java.util.Set;

/**
 * implementation of reconnecting factory for java applications.
 */
public class ReefReconnectingFactory implements ReconnectingConnectionFactory, org.totalgrid.reef.client.sapi.client.rest.ConnectionWatcher
{

    private Set<ConnectionWatcher> watchers = new HashSet<ConnectionWatcher>();
    private final BrokerConnectionFactory brokerConnectionFactory;
    private final ExecutorService exe;
    private final ServicesList servicesList;

    private final DefaultReconnectingFactory factory;

    /**
     * @param settings amqp settings
     * @param list services list from service-client package
     * @param startDelayMs beginning delay if can't connect first time
     * @param maxDelayMs delay doubles in length upto this maxTime
     */
    public ReefReconnectingFactory( AmqpSettings settings, ServicesList list, long startDelayMs, long maxDelayMs )
    {
        brokerConnectionFactory = new QpidBrokerConnectionFactory( settings );
        exe = Executors.newResizingThreadPool( new Minutes( 5 ) );
        servicesList = list;
        factory = new DefaultReconnectingFactory( brokerConnectionFactory, exe, startDelayMs, maxDelayMs );
        factory.addConnectionWatcher( this );
    }

    public synchronized void onConnectionClosed( boolean expected )
    {
        for ( ConnectionWatcher cw : watchers )
        {
            cw.onConnectionClosed( expected );
        }
    }

    public synchronized void onConnectionOpened( BrokerConnection connection )
    {
        org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection scalaConnection;
        scalaConnection = new DefaultConnection( connection, exe, 5000 );
        scalaConnection.addServicesList( servicesList );
        Connection c = new ConnectionWrapper( scalaConnection, exe );
        for ( ConnectionWatcher cw : watchers )
        {
            cw.onConnectionOpened( c );
        }
    }

    public synchronized void addConnectionWatcher( ConnectionWatcher watcher )
    {
        watchers.add( watcher );
    }

    public synchronized void removeConnectionWatcher( ConnectionWatcher watcher )
    {
        watchers.remove( watcher );
    }

    public void start()
    {
        factory.start();
    }

    public void stop()
    {
        factory.stop();
        exe.terminate();
    }
}
