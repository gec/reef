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
 * When using a ReconnectingFactory an application will register itself as a connection
 * watcher. When a connection to the broker is established the ReconenctingFactory will
 * pass the conenction to the user in the onConnectionOpened call. This conenction will
 * be valid and usable and should replace any previous connections being used. Applciations
 * should consider an unexpected onConnectionClosed event to be catastrophic and the
 * application should revert to the same behaviors it would have before it had connected
 * originally to the server. All Subscriptions and clients spawned from the previous
 * client are now invalid. Remember that AuthTokens are not tied to a particular client so
 * if an application had stored the AuthTokens they could be reused when the connection to
 * broker is reacquired.
 */
public interface ConnectionWatcher
{

    /**
     * called when we lose connection to the broker. All subscriptions and clients spawned
     * from the previous connection are now invalid.
     *
     * @param expected True if the close was user initiated, false otherwise
     */
    void onConnectionClosed( boolean expected );

    /**
     * called with the new connection we have made to the server, all of the service
     * configurations and rpcInterfaces will have been configured by Factory class
     */
    void onConnectionOpened( Connection connection );

}