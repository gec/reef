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
    private final ExecutorService exe;
    private final ServicesList servicesList;

    /**
     * @param settings amqp settings
     * @param list services list from service-client package
     */
    public ReefConnectionFactory( AmqpSettings settings, ServicesList list )
    {
        brokerConnectionFactory = new QpidBrokerConnectionFactory( settings );
        exe = Executors.newScheduledThreadPool( 5 );
        servicesList = list;
    }

    public Connection connect() throws ReefServiceException
    {
        org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection scalaConnection;
        scalaConnection = new DefaultConnection( brokerConnectionFactory.connect(), exe, 5000 );
        scalaConnection.addServicesList( servicesList );
        return new ConnectionWrapper( scalaConnection );
    }

    public void terminate()
    {
        exe.terminate();
    }
}
