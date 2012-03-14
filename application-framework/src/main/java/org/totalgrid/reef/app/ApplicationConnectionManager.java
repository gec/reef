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
package org.totalgrid.reef.app;

import org.totalgrid.reef.client.Connection;
import org.totalgrid.reef.client.exception.ReefServiceException;

/**
 * ApplicationConnectionManager manages the connection state to reef. This is more than just the connection to the broker,
 * it also requires that the server is responding to heartbeats from the application. We are only "fully connected"
 * if we have a valid broker connection, were able to login the application level user, register the application
 * and be successfully making heartbeats. If any of those conditions are not met the rest of the application should
 * treat reef as unreachable.
 *
 * Contract:
 *
 * If we are "fully connected" to reef, getConnection() will return a usable reef Connection.
 * If we are not "fully connected" then we will throw an exception if getConnection is called.
 * User code should use isConnected() to see if getConnection will throw (but they will still need
 * to be ready to catch a ReefConnectionClosedException that may occur from outside our control).
 * If user code needs to perform work if the state changes they should register for updates using
 * addConnectionListener. The callbacks will come on a random Executor pool thread.
 */
public interface ApplicationConnectionManager
{
    /**
     * starts connection process, must only be called once by a single management thread.
     */
    void start();

    /**
     * blocks until connection manager has totally stopped. Once stopped a connection manager cannot be
     * restarted
     */
    void stop();

    /**
     * whether we are "fully connected" or not
     */
    boolean isConnected();

    /**
     * if we are shutting down
     */
    boolean isShutdown();

    /**
     * gets the current connection or throws an exception if not connected
     */
    Connection getConnection() throws ReefServiceException;

    /**
     * adds a listener, if manager is already connected we will immediately call onConnected
     * with current state of manager.
     */
    void addConnectionListener( ApplicationConnectionListener listener );

    /**
     * removes a listener
     */
    void removeConnectionListener( ApplicationConnectionListener listener );
}
