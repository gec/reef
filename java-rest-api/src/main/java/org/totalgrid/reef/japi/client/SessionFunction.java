/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.japi.client;

import org.totalgrid.reef.japi.ReefServiceException;

/**
 * Function that does some work with the supplied <code>Session</code>.  The <code>Session</code> should be used only for the duration
 * of the <code>apply()</code> method call.
 */
public interface SessionFunction<A> {

    /**
     * @param session a session to use for the function
     * @return the result of the function
     * @throws ReefServiceException
     */
    A apply( Session session ) throws ReefServiceException;
}
