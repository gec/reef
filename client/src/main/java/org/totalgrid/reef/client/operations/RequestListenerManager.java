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
package org.totalgrid.reef.client.operations;

/**
 * We can attach a listener that is notified of every request low-level to the server
 * this allows creating useful logging or state tracking objects.
 *
 * Requests listeners are client-only state and are not copied by spawn.
 */
public interface RequestListenerManager
{
    /**
     * @param listener new listener to add, adding the same listener more than once has no effect
     */
    void addRequestListener( RequestListener listener );

    /**
     * @param listener listener to remove, silently ignores a remove of an unknown object
     */
    void removeRequestListener( RequestListener listener );
}
