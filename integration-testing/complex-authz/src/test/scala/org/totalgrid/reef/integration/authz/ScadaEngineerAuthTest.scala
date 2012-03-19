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
package org.totalgrid.reef.integration.authz

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection

@RunWith(classOf[JUnitRunner])
class ScadaEngineerAuthTest extends AuthTestBase {

  test("Scada engineer can't add users or issue commands") {
    as("scada") { scada =>
      unAuthed("scada cant make agents") { scada.createNewAgent("agent", "agent", List("all")).await }

      val cmd = scada.getCommands.await.head
      unAuthed("scada cant issue commands") { scada.createCommandExecutionLock(cmd).await }
    }
  }

  test("Scada engineer can disable/enable endpoints but not update state") {
    as("scada") { scada =>
      val endpointConnection = scada.getEndpointConnections().await.head
      val endpointUuid = endpointConnection.getEndpoint.getUuid

      scada.disableEndpointConnection(endpointUuid).await.getEnabled should equal(false)

      unAuthed("Engineer can't update endpoint state") {
        scada.alterEndpointConnectionState(endpointConnection.getId, EndpointConnection.State.ERROR).await
      }

      as("fep_app") { fep =>
        fep.alterEndpointConnectionState(endpointConnection.getId, EndpointConnection.State.ERROR).await
      }

      scada.enableEndpointConnection(endpointUuid).await.getEnabled should equal(true)
    }
  }
}
