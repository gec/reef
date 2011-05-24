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
package org.totalgrid.reef.api.javaclient;

import org.totalgrid.reef.api.ExpectationException;

/**
 * Interfaces that defines a response to service request
 * @param <A> The return type of the service request
 */
public interface Response<A> {

   /**
     * @return True if the service request was successful, false otherwise
     */
  boolean isSuccess();

   /**
     * Interprets the result as a successful request with 0 or more return values
     * @return A list of return values
     * @throws ExpectationException if the response is not a success
     */
  java.util.List<A> expectMany() throws ExpectationException;

   /**
     * Interprets the result as a successful request with exactly 1 return value
     * @return A single value
     * @throws ExpectationException if the response or some number of return values other than 1
     */
  A expectOne() throws ExpectationException;

}
