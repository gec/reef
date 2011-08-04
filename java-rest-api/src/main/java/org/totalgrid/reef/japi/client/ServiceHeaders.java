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

/**
 * Interface that defines what operations can be done to service header
 */
public interface ServiceHeaders
{

    /**
     * Sets the AuthToken field in the header
     * @param token A string representing the auth token
     */
    void setAuthToken( String token );

    /**
     * Clears the AuthToken field in the header
     */
    void clearAuthToken();

    /**
     * overrides the system default for maximum # of results to return at a time
     * @param resultLimit must be a positive integer
     */
    void setResultLimit( int resultLimit );

    /**
     * unset the result limit to return to using the system default
     */
    void clearResultLimit();
}
