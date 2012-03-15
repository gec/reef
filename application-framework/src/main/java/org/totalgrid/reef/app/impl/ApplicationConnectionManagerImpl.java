/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/agpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.app.impl;

import net.agileautomata.executor4s.ExecutorService;
import net.agileautomata.executor4s.Executors;
import net.agileautomata.executor4s.Minutes;
import org.totalgrid.reef.app.ApplicationConnectionListener;
import org.totalgrid.reef.app.ApplicationConnectionManager;
import org.totalgrid.reef.app.ApplicationSettings;
import org.totalgrid.reef.app.ConnectedApplication;
import org.totalgrid.reef.app.ConnectionCloseManagerEx;
import org.totalgrid.reef.client.Connection;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.exception.ServiceIOException;
import org.totalgrid.reef.client.javaimpl.ConnectionWrapper;
import org.totalgrid.reef.client.sapi.client.rest.Client;
import org.totalgrid.reef.client.service.proto.Application;
import org.totalgrid.reef.client.settings.AmqpSettings;
import org.totalgrid.reef.client.settings.NodeSettings;
import org.totalgrid.reef.client.settings.UserSettings;

import java.util.LinkedList;
import java.util.List;


public class ApplicationConnectionManagerImpl implements ApplicationConnectionManager, ConnectedApplication
{
    private final ExecutorService executor;
    private ConnectionCloseManagerEx connectionManager;
    private SimpleConnectedApplicationManager applicationManager;
    private ApplicationSettings applicationSettings;

    private volatile Connection connection = null;
    private volatile boolean shutdown = false;
    private List<ApplicationConnectionListener> listeners = new LinkedList<ApplicationConnectionListener>();

    public ApplicationConnectionManagerImpl( AmqpSettings amqpSettings, UserSettings userSettings, NodeSettings nodeSettings, String instanceName,
        String capability )
    {
        executor = Executors.newResizingThreadPool( new Minutes( 5 ) );
        connectionManager = new ConnectionCloseManagerEx( amqpSettings, executor );
        ApplicationManagerSettings mangerSettings = new ApplicationManagerSettings( userSettings, nodeSettings );
        applicationManager = new SimpleConnectedApplicationManager( executor, connectionManager, mangerSettings );
        applicationSettings = new ApplicationSettings( instanceName, capability );
    }

    public ApplicationSettings getApplicationSettings()
    {
        return applicationSettings;
    }

    public synchronized void onApplicationStartup( Application.ApplicationConfig appConfig,
        org.totalgrid.reef.client.sapi.client.rest.Connection newConnection, Client scalaClient )
    {
        connection = new ConnectionWrapper( newConnection, executor );

        notifyListeners( true );
    }

    public synchronized void onApplicationShutdown()
    {
        connection = null;

        notifyListeners( false );
    }

    public synchronized void onConnectionError( String msg )
    {
        for ( ApplicationConnectionListener listener : listeners )
        {
            listener.onConnectionError( msg );
        }
    }

    public void start()
    {
        if ( shutdown )
            throw new IllegalArgumentException( "Manager cannot be restarted." );

        connectionManager.start();
        applicationManager.start();

        applicationManager.addConnectedApplication( this );
    }

    public void stop()
    {
        shutdown = true;

        applicationManager.removeConnectedApplication( this );

        applicationManager.stop();
        connectionManager.stop();

        executor.terminate();
    }

    public synchronized boolean isConnected()
    {
        return !shutdown && connection != null;
    }

    public boolean isShutdown()
    {
        return shutdown;
    }

    public synchronized Connection getConnection() throws ReefServiceException
    {
        if ( !isConnected() )
            throw new ServiceIOException( "Not connected to reef" );
        return connection;
    }

    public synchronized void addConnectionListener( ApplicationConnectionListener listener )
    {
        listeners.remove( listener );
        listeners.add( listener );
        listener.onConnectionStatusChanged( isConnected() );
    }

    public synchronized void removeConnectionListener( ApplicationConnectionListener listener )
    {
        listeners.remove( listener );
    }

    private void notifyListeners( boolean status )
    {
        for ( ApplicationConnectionListener listener : listeners )
        {
            listener.onConnectionStatusChanged( status );
        }
    }
}
