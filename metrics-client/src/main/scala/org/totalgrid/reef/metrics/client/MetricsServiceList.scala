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
package org.totalgrid.reef.metrics.client

import impl.MetricsServiceImpl
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.types.{ ServiceInfo, ServiceTypeInformation }
import org.totalgrid.reef.client.{ ServiceProviderFactory, Client, ServiceProviderInfo, ServicesList }

class MetricsServiceList extends ServicesList {

  def getServiceTypeInformation: java.util.List[ServiceTypeInformation[_, _]] = {
    val typ = new MetricsReadDescriptor
    List(new ServiceInfo(typ, typ))
  }

  def getServiceProviders: java.util.List[ServiceProviderInfo] = {
    List(new MetricsProviderInfo)
  }

  class MetricsProviderInfo extends ServiceProviderInfo {

    def getFactory = {
      new ServiceProviderFactory {
        def createRpcProvider(client: Client): AnyRef = {
          new MetricsServiceImpl(client.asInstanceOf[Client])
        }
      }
    }

    def getInterfacesImplemented: java.util.List[Class[_]] = {
      List(classOf[MetricsService])
    }
  }
}