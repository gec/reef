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
package org.totalgrid.reef.api.sapi.client

import org.totalgrid.reef.api.japi.client.{ SubscriptionBinding, SubscriptionCreationListener, SubscriptionCreator }

trait SubscriptionCreatorManager extends SubscriptionCreator {

  private var listeners = Set.empty[SubscriptionCreationListener]

  def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = this.synchronized(listeners += listener)
  def removeSubscriptionCreationListener(listener: SubscriptionCreationListener) = this.synchronized(listeners -= listener)

  // TODO: this should be protected for use by defaultAnnotatedOperations only
  def onSubscriptionCreated(binding: SubscriptionBinding) = listeners.foreach { _.onSubscriptionCreated(binding) }
}