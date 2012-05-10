package org.totalgrid.reef.client.javaimpl

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

import fixture._
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.client.AnyNodeDestination
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.{ Connection, Client }
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.ServiceIOException
import net.agileautomata.executor4s._
import SimpleRestAccess._
import org.totalgrid.reef.client.factory.ReefConnectionFactory

@RunWith(classOf[JUnitRunner])
class QpidConnectionShutdownTests extends ConnectionShutdownTests with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryConnectionShutdownTests extends ConnectionShutdownTests with MemoryBrokerTestFixture

// provides a specification for how the client should interact with brokers. testable on multiple brokers via minx
trait ConnectionShutdownTests extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture(fun: (Client, Connection) => Unit) {

    val (brokerFac, kill) = getFactory
    val exe = Executors.newResizingThreadPool(5.minutes)
    val fac = new ReefConnectionFactory(brokerFac, exe, ExampleServiceList)

    try {
      val conn = fac.connect()

      // we add and then cancel a service binding just to create the exchanges
      val binding = conn.getServiceRegistration.bindService(new BlackHoleService(SomeIntegerTypeDescriptor), SomeIntegerTypeDescriptor, new AnyNodeDestination, true)
      binding.cancel()

      fun(conn.createClient("foo"), conn)
    } finally {
      exe.terminate()
      kill()
    }
  }

  test("Outstanding requests are killed on disconnect") {
    fixture { (c, conn) =>
      val i = SomeInteger(1)
      val f = c.put(i)
      conn.disconnect()
      f.await.getStatus should equal(Envelope.Status.BUS_UNAVAILABLE)
    }
  }

  test("Requests cannot be made after disconnection") {
    fixture { (c, conn) =>
      conn.disconnect()
      val i = SomeInteger(1)
      val f = c.put(i)
      f.await.getStatus should equal(Envelope.Status.BUS_UNAVAILABLE)
    }
  }

  test("Bind service after disconnect fails") {
    fixture { (c, conn) =>
      conn.disconnect()
      intercept[ServiceIOException] {
        conn.getServiceRegistration.bindService(new BlackHoleService(SomeIntegerTypeDescriptor), SomeIntegerTypeDescriptor, new AnyNodeDestination, true)
      }
    }
  }

  test("Subscribe fails after disconnect") {
    fixture { (c, conn) =>
      conn.disconnect()
      intercept[ServiceIOException] {
        c.subscribe(SomeIntegerTypeDescriptor)
      }
    }
  }
}