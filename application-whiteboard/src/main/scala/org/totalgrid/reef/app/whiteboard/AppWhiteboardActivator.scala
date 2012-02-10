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
package org.totalgrid.reef.app.whiteboard

import org.osgi.framework.BundleContext
import net.agileautomata.executor4s.Executor
import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.app._

/**
 * the app whiteboard may be useful but should only be used with applications that really don't care what
 * order they are invoked in. More complex applications should use ConnectedApplicationBundleActivator so
 * they are correctly started/stopped by OSGI.
 */
class AppWhiteboardActivator extends ConnectedApplicationBundleActivator {

  def addApplication(context: BundleContext, connectionManager: ConnectionProvider, appManager: ConnectedApplicationManager, executor: Executor) {
    context watchServices withInterface[ConnectionConsumer] andHandle {
      case AddingService(p, _) => connectionManager.addConsumer(p)
      case ServiceRemoved(p, _) => connectionManager.removeConsumer(p)
    }

    context watchServices withInterface[ConnectedApplication] andHandle {
      case AddingService(p, _) => appManager.addConnectedApplication(p)
      case ServiceRemoved(p, _) => appManager.removeConnectedApplication(p)
    }
  }

}