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
package org.totalgrid.reef.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.io.{ IOException, File }

@RunWith(classOf[JUnitRunner])
class IOHelperTest extends FunSuite with ShouldMatchers {
  test("Read and write binary file") {
    val tmp = File.createTempFile("tempTestFile", "ext")

    val array = (0 to 100).map { _.toByte }.toArray

    IOHelpers.writeBinary(tmp, array)
    val read = IOHelpers.readBinary(tmp)

    read should equal(array)
  }

  test("Read and write text file") {
    val tmp = File.createTempFile("tempTestFile2", "ext")

    val string = "sdasdasd\nasdasdasdas\r\nasdsadasdsadasd\r7389473298473298748932\r\n\r\n"

    IOHelpers.writeString(tmp, string)
    val read = IOHelpers.readString(tmp)

    read should equal(string)
  }

  test("Throws on bad files") {
    val fname = "678687687687"
    val badFile = new File("/" + fname)

    val ex = intercept[IOException] {
      IOHelpers.readString(badFile)
    }
    ex.getMessage should include(fname)

    intercept[IOException] {
      IOHelpers.writeString(badFile, "blah")
    }
  }
}