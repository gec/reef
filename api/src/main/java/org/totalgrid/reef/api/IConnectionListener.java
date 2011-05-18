package org.totalgrid.reef.api

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
/**
 * It is important to use an IConnectionListener to be informed of disconnections from the message broker (expected
 * or otherwise). Callbacks come in from the messaging thread so it is important not to block the callbacks.
 */
trait IConectionListener {
  /**
   * called when we lose connection to the broker. This means all subscriptions spawned from the IConnection during
   * this time are invalid and need to be thrown away.
   */
  def closed()
  /**
   * called when we have established a connection to the message broker, we can now provide ISessions
   */
  def opened()

  // TODO: write tests to figure out what can and can't be done inside IConnectionListener callbacks reef_techdebt-7
  // TODO: add exception callback for ISubscription and possibly ISession
}