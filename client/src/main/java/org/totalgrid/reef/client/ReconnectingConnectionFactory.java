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

/**
 * The standard ConnectionFactory is designed to provide a single Connection
 * and assumes the conection to the broker will last for the life of the
 * application. For many single-shot applications this is a reasnoble constraint
 * but for longer lived applications, like HMIs and proxies, the application
 * should be tolerant to the coming and going to the message broker. A
 * ReconnectingConnectionFactory will automatically try making a connection
 * to the broker until it succeeds and re-attempt the connection if the broker
 * is lost. Losing the connection means all Subscriptions, clients and rpc interfaces
 * are now invalid, see the ConnectionWatcher interface for correct application behavior.
 */
public interface ReconnectingConnectionFactory
{
    /**
     * add a connection watcher
     * @param watcher
     */
    void addConnectionWatcher( ConnectionWatcher watcher );

    /**
     * remove a connection watcher
     * @param watcher
     */
    void removeConnectionWatcher( ConnectionWatcher watcher );

    /**
     * kicks off the connect attempts to the broker.
     */
    void start();

    /**
     * Stops any outstanding connection attempts if it hasn't connected and
     * disconnects the broker connection. Applications should not use the
     * onConnectionClosed() callback for normal shutdown operations since
     * the connection to the server is already lost when the callback fires.
     */
    void stop();
}
