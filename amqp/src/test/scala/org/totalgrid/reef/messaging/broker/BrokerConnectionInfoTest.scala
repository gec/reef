package org.totalgrid.reef.messaging.broker

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
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class BrokerConnectionInfoTest extends FunSuite with ShouldMatchers {

  def deleteTestFile(testFunction: () => Unit) {
    try {
      //val file = new File("test.yml")
      //file.delete
      testFunction
    } finally { //val file = new File("test.yml")
      //file.delete
    }
  }

  test("Default Broker Info is populated") {
    //val fw = new FileWriter()

    deleteTestFile { () =>
      println("tyy")
      val b = BrokerConnectionInfo.loadInfo("development")
      b.host should equal("127.0.0.1")
      b.port should equal(5672)

    }
  }

  test("Bad enviroment causes exception") {
    deleteTestFile { () =>
      intercept[Exception] {
        BrokerConnectionInfo.loadInfo("magic")
      }
    }
  }

}
