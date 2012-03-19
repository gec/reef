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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.service.proto.Utils.Attribute

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Alarms.EventConfig
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class EventConfigRequestTest extends ServiceClientSuite {

  test("Create Log, Event, Alarm configurations") {

    var configs = List.empty[EventConfig]

    configs ::= client.setEventConfigAsLogOnly("Demo.AsLog", 1, "Log Message").await

    configs ::= client.setEventConfigAsEvent("Demo.AsEvent", 2, "Event Message").await

    configs ::= client.setEventConfigAsEvent("Demo.AsAlarm", 3, "Alarm Message").await

    client.publishEvent("Demo.AsEvent", "Tests").await

    client.publishEvent("Demo.AsLog", "Tests").await

    configs ::= client.setEventConfigAsEvent("Demo.Formatting", 1, "Attributes name: {name} value: {value}").await

    client.publishEvent("Demo.Formatting", "Tests", makeAttributeList("name" -> "abra", "value" -> "cadabra")).await

    configs.foreach(client.deleteEventConfig(_).await)
  }

  def makeAttributeList(tuples: Tuple2[String, String]*): List[Attribute] = {
    tuples.map {
      case (name, value) =>
        Attribute.newBuilder.setName(name).setValueString(value).setVtype(Attribute.Type.STRING).build
    }.toList
  }

}