package org.totalgrid.reef.client.factory

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
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.clientapi.exceptions.ReefServiceException

import org.totalgrid.reef.clientapi.{ ConnectionFactory, Connection }

import org.totalgrid.reef.clientapi.settings.AmqpSettings
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.clientapi.javaimpl.ConnectionWrapper
import org.totalgrid.reef.clientapi.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.clientapi.rpc.ServicesList

class ReefConnectionFactory(settings: AmqpSettings, servicesList: ServicesList) extends ConnectionFactory {
  private val factory = new QpidBrokerConnectionFactory(settings)
  private val exe = Executors.newScheduledThreadPool(5)

  @throws(classOf[ReefServiceException])
  def connect(): Connection = {
    val connection = new DefaultConnection(factory.connect, exe, 5000)
    connection.addServicesList(servicesList)
    new ConnectionWrapper(connection)
  }

  def terminate() = exe.terminate()
}
