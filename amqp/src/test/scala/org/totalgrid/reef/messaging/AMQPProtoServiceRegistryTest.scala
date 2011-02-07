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
import org.totalgrid.reef.messaging.ServicesList.UnknownServiceException

import org.totalgrid.reef.protoapi.ProtoServiceException

@RunWith(classOf[JUnitRunner])
class AMQPProtoServiceRegistryTest extends Suite with ShouldMatchers {

  def testFooExists() {
    val exchangeMap: Map[Class[_], ServiceInfo] = Map(
      classOf[Example.Foo] -> ServiceInfo("foo", Deserializers.foo, false, Deserializers.foo))

    val factory = new AMQPProtoFactory with ReactActor {
      val broker = new MockBrokerInterface
    }
    val registry = new AMQPProtoRegistry(factory, 0, ServicesList.getServiceInfo(_, exchangeMap))
    registry.getServiceClient(Example.Foo.parseFrom)

    val client = new ProtoClient(factory, 0, ServicesList.getServiceInfo(_, exchangeMap))
    intercept[ProtoServiceException] {
      client.getOneThrow(Example.Foo.newBuilder.build)
    }
  }

  def testUnknownTypeThrowsException() {
    val exchangeMap = Map.empty[Class[_], ServiceInfo]
    val factory = new AMQPProtoFactory with ReactActor {
      val broker = new MockBrokerInterface
    }

    val registry = new AMQPProtoRegistry(factory, 0, ServicesList.getServiceInfo(_, exchangeMap))
    intercept[UnknownServiceException] {
      registry.getServiceClient(Example.Foo.parseFrom)
    }

    val client = new ProtoClient(factory, 0, ServicesList.getServiceInfo(_, exchangeMap))
    intercept[UnknownServiceException] {
      client.getOneThrow(Example.Foo.newBuilder.build)
    }
  }

}

