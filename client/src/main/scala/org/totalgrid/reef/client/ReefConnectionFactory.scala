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
package org.totalgrid.reef.client

import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.clientapi.exceptions.ReefServiceException

import org.totalgrid.reef.clientapi.{ ConnectionFactory, Connection }

import org.totalgrid.reef.clientapi.settings.AmqpSettings
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.client.sapi.ReefServices
import org.totalgrid.reef.clientapi.javaimpl.ConnectionWrapper

class ReefConnectionFactory(settings: AmqpSettings) extends ConnectionFactory {
  private val factory = new QpidBrokerConnectionFactory(settings)
  private val exe = Executors.newScheduledThreadPool(5)

  @throws(classOf[ReefServiceException])
  def connect(): Connection = new ConnectionWrapper(ReefServices.apply(factory.connect, exe))

  def terminate() = exe.terminate()
}
