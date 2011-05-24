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
 * A thread-safe interface for executing arbitrary blocks of code on against of pool of Session's
 */
public interface SessionExecutionPool {

  /**
   * Executes a block of code using a temporarily acquired session cleaning up any affected state afterwards
   * @consumer a block of code to execute using the acquired Session
   * @return the return value from consumer.apply
   * @throws ReefServiceException if a session cannot be acquired we will throw an error
   */
  <A> A execute(SessionFunction<A> function) throws ReefServiceException;

  /**
   * Executes a block of code using a temporarily acquired session cleaning up any affected state afterwards
   * @param authToken an authtoken to attach before calling consumer.apply
   * @param function a block of code to execute using the acquired Session
   * @return the return value from consumer.apply
   * @throws ReefServiceException if a session cannot be acquired we will throw an error
   */
  <A> A execute(String authToken, SessionFunction<A> function) throws ReefServiceException;

}