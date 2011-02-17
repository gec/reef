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
package org.totalgrid.reef.api.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ Point }

@RunWith(classOf[JUnitRunner])
class PointRequestTest
    extends ServiceClientSuite("Point.xml", "Point",
      <div>
        <p>
          A Point represents a configured input point for data acquisition. Measurements associated with
      this point all use the point name.
        </p>
        <p>
          Every Point is associated with an Entity of type "Point". The point's location in the system
      model is determined by this entity. Points are also associated with entities designated as
      "logical nodes", which represent the communications interface/source.
        </p>
      </div>)
    with ShouldMatchers {

  test("Simple gets") {

    val allReq = Point.newBuilder.setUid("*").build
    val allResp = client.getOrThrow(allReq)

    doc.addCase("Get all", "Get", "Get all Points", allReq, allResp)

    val uidReq = Point.newBuilder.setUid(allResp.head.getUid).build
    val uidResp = client.getOneOrThrow(uidReq)

    doc.addCase("Get by UID", "Get", "Get point that matches a certain UID.", uidReq, uidResp)

    val nameReq = Point.newBuilder.setName(allResp.head.getName).build
    val nameResp = client.getOneOrThrow(nameReq)

    doc.addCase("Get by name", "Get", "Get point that matches a certain name.", nameReq, nameResp)
  }

  /*test("Abnormal") {
    val putReq = Point.newBuilder.setName("StaticSubstation.Breaker02.Bkr").setAbnormal(true).build
    val putResp = client.putOneOrThrow(putReq)

    doc.addCase("Set to abnormal", "Put", "Mark a point as being in an abnormal state.", putReq, putResp)

  }*/
}