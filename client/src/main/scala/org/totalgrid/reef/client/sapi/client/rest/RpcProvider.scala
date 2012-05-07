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
package org.totalgrid.reef.client.sapi.client.rest

import org.totalgrid.reef.client.{ ServiceProviderFactory, ServiceProviderInfo, Client => JClient }

trait RpcProvider {
  def getRpcInterface[A](klass: Class[A]): A
}

/**
 * helper object to make defining a provider nice looking in scala
 */
object RpcProvider {
  import scala.collection.JavaConversions._

  def apply(fun: (JClient) => AnyRef, interfaces: List[Class[_]]): ServiceProviderInfo = {
    new ServiceProviderInfo {
      def getInterfacesImplemented: java.util.List[Class[_]] = interfaces

      def getFactory = new ServiceProviderFactory {
        def createRpcProvider(client: JClient): AnyRef = fun(client)
      }
    }
  }
}