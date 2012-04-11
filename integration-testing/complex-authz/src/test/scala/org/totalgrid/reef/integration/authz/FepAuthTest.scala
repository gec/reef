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
class FepAuthTest extends AuthTestBase {

  val USER = "fep_application"

  test("fep user can update state") {
    as(USER) { fep =>
      val endpointConnection = fep.getEndpointConnections().await.head
      val endpointUuid = endpointConnection.getEndpoint.getUuid

      fep.alterEndpointConnectionState(endpointConnection.getId, EndpointConnection.State.ERROR).await

      fep.alterEndpointConnectionState(endpointConnection.getId, EndpointConnection.State.COMMS_UP).await

      unAuthed("Fep can't update endpoint enabled") {
        fep.disableEndpointConnection(endpointUuid).await.getEnabled should equal(false)
      }
    }
  }
}
