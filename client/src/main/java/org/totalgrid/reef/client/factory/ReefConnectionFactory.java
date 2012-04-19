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

import net.agileautomata.executor4s.Executor;
import net.agileautomata.executor4s.ExecutorService;
import net.agileautomata.executor4s.Executors;
import net.agileautomata.executor4s.Minutes;
import org.totalgrid.reef.broker.BrokerConnectionFactory;
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory;
import org.totalgrid.reef.client.Connection;
import org.totalgrid.reef.client.ConnectionFactory;
import org.totalgrid.reef.client.ServicesList;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.javaimpl.ConnectionWrapper;
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection;
import org.totalgrid.reef.client.settings.AmqpSettings;

/**
 * Implementation of a "single-shot" ConnectionFactory that make the underlying connection
 * and return a Connection object with a the passed in ServicesList already attached.
 */
public class ReefConnectionFactory implements ConnectionFactory
{
    private final BrokerConnectionFactory brokerConnectionFactory;
    private final ExecutorService exeService;
    private final Executor exe;
    private final ServicesList servicesList;

    /**
     * @param settings Settings for AMQP connection
     * @param list services list from service-client package
     * @return
     */
    public static ConnectionFactory defaultFactory( AmqpSettings settings, ServicesList list )
    {
        BrokerConnectionFactory broker = new QpidBrokerConnectionFactory( settings );
        return new ReefConnectionFactory( broker, list );
    }

    /**
     * @param brokerConnectionFactory broker connection
     * @param exe Executor to use
     * @param list services list from service-client package
     */
    public ReefConnectionFactory( BrokerConnectionFactory brokerConnectionFactory, Executor exe, ServicesList list )
    {
        this.brokerConnectionFactory = brokerConnectionFactory;
        this.exe = exe;
        this.exeService = null;
        this.servicesList = list;
    }

    /**
     * @param brokerConnectionFactory broker connection
     * @param list services list from service-client package
     */
    public ReefConnectionFactory( BrokerConnectionFactory brokerConnectionFactory, ServicesList list )
    {
        this.brokerConnectionFactory = brokerConnectionFactory;
        this.exeService = Executors.newResizingThreadPool( new Minutes( 5 ) );
        this.exe = exeService;
        this.servicesList = list;
    }

    public Connection connect() throws ReefServiceException
    {
        org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection scalaConnection;
        scalaConnection = new DefaultConnection( brokerConnectionFactory.connect(), exe, 5000 );
        scalaConnection.addServicesList( servicesList );
        return new ConnectionWrapper( scalaConnection, exe );
    }

    public void terminate()
    {
        if ( exeService != null )
        {
            exeService.terminate();
        }
    }
}
