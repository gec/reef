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


import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.settings.UserSettings;

/**
 *
 * A Connection is an established, but not yet authorized, link with a Reef node. It provides 2 ways to login:
 *
 * 1) Using a defined username/password
 * 2) Using a pre-acquired authorization token
 *
 * It is recommended that most applications read the UserSettings from a Java properties file and authenticate
 * using option 1).
 *
 * Connection is thread-safe
 *
 */
public interface Connection
{

    /**
     * register a listener for open/close events
     *
     * @param listener Interace to call back with open/close events
     */
    void addConnectionListener( ConnectionCloseListener listener );

    /**
     * remove a listener for open/close events
     *
     * @param listener Interace to call back with open/close events
     */
    void removeConnectionListener( ConnectionCloseListener listener );

    /**
     * Attempt to login to Reef using a UserSettings object.
     * @param userSettings UserSettings object that defines username/password
     * @return An authorized client
     * @throws ReefServiceException If the login fails for any reason
     */
    Client login( UserSettings userSettings ) throws ReefServiceException;

    /**
     * Login to reef using a pre-acquired authentication token
     * @param authToken pre-acquired authentication token
     * @return An authorized client
     */
    Client login( String authToken );


    /**
     * Terminates the connection. ConnectionCloseListeners will receive "expected" = true
     */
    void disconnect();

    void addServicesList( ServicesList servicesList );
}
