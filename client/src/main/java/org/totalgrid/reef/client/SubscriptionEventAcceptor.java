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
 * Classes that want to consume subscription events should implement this interface.
 */
public interface SubscriptionEventAcceptor<A>
{

    /**
     * Called on every received event that each call to this function will come in from
     * an unknown thread but no more than one call at a time will occur at a time, the
     * events will be pushed in one at a time.
     *
     * The onEvent handler shouldn't throw exceptions since they will be hard to track down
     * as the stacktrace will be all implementation code. If an exception is thrown it will
     * be logged and then discarded, it doesn't cancel the subscription.
     *
     * @param event message that contains code and the payload object
     */
    void onEvent( SubscriptionEvent<A> event );

}
