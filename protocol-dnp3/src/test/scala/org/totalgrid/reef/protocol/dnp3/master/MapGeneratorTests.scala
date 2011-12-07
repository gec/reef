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
package org.totalgrid.reef.protocol.dnp3.master

import org.totalgrid.reef.client.service.proto.Mapping

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MapGeneratorTests extends Suite with ShouldMatchers {

  def testEmptyProto() {
    val map = Mapping.IndexMapping.newBuilder
    map.setDeviceId("test")
    val x = MapGenerator.getMeasMap(map.build)
    x.size should equal(0)
  }

  def testSimpleEntries() {
    val map = Mapping.IndexMapping.newBuilder.setDeviceId("test")
    val assoc = Mapping.MeasMap.newBuilder.setIndex(0).setPointName("meas1").setUnit("raw").setType(Mapping.DataType.BINARY)
    map.addMeasmap(assoc)
    val x = MapGenerator.getMeasMap(map.build)
    x.size should equal(1)
    val entry = x((0, Mapping.DataType.BINARY.getNumber))
    entry.getUnit should equal("raw")
    entry.getPointName should equal("meas1")
  }

}

