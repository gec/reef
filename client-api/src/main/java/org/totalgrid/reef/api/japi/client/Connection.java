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
package org.totalgrid.reef.api.japi.client;


/**
 * Thread safe connection handler to connect to the greenbus, handles the starting and stopping
 * of the connection and provides a factory to create service clients.
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
     * Terminates the connection. ConnectionCloseListeners will receive "expected" = true
     */
    boolean disconnect();



}
