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
package org.totalgrid.reef.japi.client;

import java.util.Properties;

import org.totalgrid.reef.japi.client.util.PropertyLoading;

/**
 * Settings class that defines properties for an AMQP connection
 */
public class AMQPConnectionSettingImpl implements AMQPConnectionSettings
{
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String virtualHost;

    private final boolean ssl;
    private final String trustStore;
    private final String trustStorePassword;

    public AMQPConnectionSettingImpl( String host, int port, String user, String password, String virtualHost )
    {
        this( host, port, user, password, virtualHost, false, null, null );
    }

    /**
     * @param host        The IP address or DNS name of AMQP broker
     * @param port        The TCP port that the broker is listening on (default 5672)
     * @param user        The username for the connection
     * @param password    The password for the connection
     * @param virtualHost The virtual host to use, default is '/'
     * @param ssl         If connection is encrypted using SSL
     * @param trustStore  Path to trustStore file (trust-store.jks)
     * @param trustStorePassword Used to verify trustStore integrity, actually closer to a checksum than password
     */
    public AMQPConnectionSettingImpl( String host, int port, String user, String password, String virtualHost, boolean ssl, String trustStore,
        String trustStorePassword )
    {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.virtualHost = virtualHost;
        this.ssl = ssl;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
    }

    /**
     * loads the connection settings from a properties object. Properties can be loaded using any of the standard
     * java methods, example below.
     * <pre>
     *   Properties props = new Properties();
     *   try
     *   {
     *     FileInputStream fis = new FileInputStream( "../org.totalgrid.reef.test.cfg" );
     *     props.load( fis );
     *     fis.close();
     *   }
     *   catch ( IOException e )
     *   {
     *     e.printStackTrace();
     *     // we'll then throw an exception when trying to load from emtpy properties file
     *   }
     *   new AMQPConnectionSettings( props );
     * </pre>
     *
     * @param props properties object loaded with appropriate org.totalgrid.reef.amqp settings
     * @throws IllegalArgumentException if needed entries are missing
     */
    public AMQPConnectionSettingImpl( Properties props ) throws IllegalArgumentException
    {
        host = PropertyLoading.getString( "org.totalgrid.reef.amqp.host", props );
        port = PropertyLoading.getInt( "org.totalgrid.reef.amqp.port", props );
        user = PropertyLoading.getString( "org.totalgrid.reef.amqp.user", props );
        password = PropertyLoading.getString( "org.totalgrid.reef.amqp.password", props );
        virtualHost = PropertyLoading.getString( "org.totalgrid.reef.amqp.virtualHost", props );
        ssl = PropertyLoading.getBoolean( "org.totalgrid.reef.amqp.ssl", props );
        if ( ssl )
        {
            trustStore = PropertyLoading.getString( "org.totalgrid.reef.amqp.trustStore", props );
            trustStorePassword = PropertyLoading.getString( "org.totalgrid.reef.amqp.trustStorePassword", props );
        }
        else
        {
            trustStore = "";
            trustStorePassword = "";
        }
    }

    @Override
    public String toString()
    {
        return ssl ? "amqps:/" + user + "@" + host + ":" + port + "/" + virtualHost + "{" + trustStore + "}" : "amqp:/" + user + "@" + host + ":"
            + port + "/" + virtualHost;
    }

    /**
     * @return host name
     */
    @Override
    public String getHost()
    {
        return host;
    }

    /**
     * @return TCP port
     */
    @Override
    public int getPort()
    {
        return port;
    }

    /**
     * @return username
     */
    @Override
    public String getUser()
    {
        return user;
    }

    /**
     * @return password
     */
    @Override
    public String getPassword()
    {
        return password;
    }

    /**
     * @return virtual host
     */
    @Override
    public String getVirtualHost()
    {
        return virtualHost;
    }

    /**
     * @return whether connection is using ssl
     */
    @Override
    public boolean getSsl()
    {
        return ssl;
    }

    /**
     * @return path of trust store file
     */
    @Override
    public String getTrustStore()
    {
        return trustStore;
    }

    /**
     * @return password for trust store password
     */
    @Override
    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

}
