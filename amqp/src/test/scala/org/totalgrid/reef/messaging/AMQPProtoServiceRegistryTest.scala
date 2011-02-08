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
package org.totalgrid.reef.messaging

import javabridge.Deserializers
import org.totalgrid.reef.proto.Example

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.messaging.mock.MockBrokerInterface
import org.totalgrid.reef.messaging.ServiceList.UnknownServiceException

import org.totalgrid.reef.protoapi.ProtoServiceException

@RunWith(classOf[JUnitRunner])
class AMQPProtoServiceRegistryTest extends Suite with ShouldMatchers {

  private def getServiceList(map: ServiceList.ServiceMap) = new ServiceListOnMap(map)

  def testFooExists() {
    val list = getServiceList(Map(
      classOf[Example.Foo] -> ServiceInfo.get("foo", Deserializers.foo)))

    val factory = new AMQPProtoFactory with ReactActor {
      val broker = new MockBrokerInterface
    }
    val registry = new AMQPProtoRegistry(factory, 0, list)
    registry.getServiceClient(Example.Foo.parseFrom)

    val client = new ProtoClient(factory, 0, list)
    intercept[ProtoServiceException] {
      client.getOneOrThrow(Example.Foo.newBuilder.build)
    }
  }

  def testUnknownTypeThrowsException() {
    val list = getServiceList(Map.empty)
    val factory = new AMQPProtoFactory with ReactActor {
      val broker = new MockBrokerInterface
    }

    val registry = new AMQPProtoRegistry(factory, 0, list)
    intercept[UnknownServiceException] {
      registry.getServiceClient(Example.Foo.parseFrom)
    }

    val client = new ProtoClient(factory, 0, list)
    intercept[UnknownServiceException] {
      client.getOneOrThrow(Example.Foo.newBuilder.build)
    }
  }

}

