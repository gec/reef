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

/**
 * Settings class that defines properties for an AMQP connection
 */
public class AMQPConnectionSettings
{
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String virtualHost;

    private final boolean ssl;
    private final String trustStore;
    private final String trustStorePassword;

    public AMQPConnectionSettings( String host, int port, String user, String password, String virtualHost )
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
    public AMQPConnectionSettings( String host, int port, String user, String password, String virtualHost, boolean ssl, String trustStore,
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
    public AMQPConnectionSettings( Properties props ) throws IllegalArgumentException
    {
        this.host = getString( "org.totalgrid.reef.amqp.host", props );
        this.port = getInt( "org.totalgrid.reef.amqp.port", props );
        this.user = getString( "org.totalgrid.reef.amqp.user", props );
        this.password = getString( "org.totalgrid.reef.amqp.password", props );
        this.virtualHost = getString( "org.totalgrid.reef.amqp.virtualHost", props );
        this.ssl = getBoolean( "org.totalgrid.reef.amqp.ssl", props );
        if ( ssl )
        {
            this.trustStore = getString( "org.totalgrid.reef.amqp.trustStore", props );
            this.trustStorePassword = getString( "org.totalgrid.reef.amqp.trustStorePassword", props );
        }
        else
        {
            this.trustStore = "";
            this.trustStorePassword = "";
        }
    }

    @Override
    public String toString()
    {
        if ( ssl )
        {
            return "amqps:/" + user + "@" + host + ":" + port + "/" + virtualHost + "{" + trustStore + "}";
        }
        else
        {
            return "amqp:/" + user + "@" + host + ":" + port + "/" + virtualHost;
        }
    }

    /**
     * @return host name
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @return TCP port
     */
    public int getPort()
    {
        return port;
    }

    /**
     * @return username
     */
    public String getUser()
    {
        return user;
    }

    /**
     * @return password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @return virtual host
     */
    public String getVirtualHost()
    {
        return virtualHost;
    }

    /**
     * @return whether connection is using ssl
     */
    public boolean getSsl()
    {
        return ssl;
    }

    /**
     * @return path of trust store file
     */
    public String getTrustStore()
    {
        return trustStore;
    }

    /**
     * @return password for trust store password
     */
    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

    // TODO: move these getX functions to a utility class
    private static String getString( String id, Properties props ) throws IllegalArgumentException
    {
        String prop = props.getProperty( id );
        if ( prop == null )
        {
            throw new IllegalArgumentException( "Could not load configuration. Missing: " + id );
        }
        return prop;
    }

    private static int getInt( String id, Properties props ) throws IllegalArgumentException
    {
        String prop = getString( id, props );
        return Integer.parseInt( prop );
    }

    private static boolean getBoolean( String id, Properties props ) throws IllegalArgumentException
    {
        String prop = getString( id, props );
        return Boolean.parseBoolean( prop );
    }
}
