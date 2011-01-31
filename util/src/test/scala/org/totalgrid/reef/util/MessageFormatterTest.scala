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
package org.totalgrid.reef.util

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created by IntelliJ IDEA.
 * User: flint
 * Date: Dec 20, 2010
 * Time: 7:14:10 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(classOf[JUnitRunner])
class MessageFormatterTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  override def beforeAll() {
  }

  test("Simple messages") {
    val alist = new AttributeList
    alist += ("a0" -> AttributeString("val0"))
    alist += ("a1" -> AttributeString("val1"))

    MessageFormatter.format("Hello", alist) should be("Hello")
    MessageFormatter.format("Hello {a0} {a1}", alist) should be("Hello val0 val1")
    MessageFormatter.format("{a0} {a1}", alist) should be("val0 val1")
    MessageFormatter.format("H{a0} {a1}", alist) should be("Hval0 val1")
    MessageFormatter.format("Hello {a0} {a1}t", alist) should be("Hello val0 val1t")
    MessageFormatter.format("{a0} {a1} tail", alist) should be("val0 val1 tail")
    MessageFormatter.format("H{a0} {a1} tail", alist) should be("Hval0 val1 tail")
  }

  /* TODO: This isn't available yet.
  test("Escape sequences") {
    val alist = new AttributeList
    alist += ("a0" -> AttributeString("a0", "val0"))
    alist += ("a1" -> AttributeString("a1", "val1"))

    MessageFormatter.format("Hello '{'a0'}' '{'a1'}'", alist) should be("Hello {a0} {a1}")
    MessageFormatter.format("'{'a0'}' {a1}", alist) should be("{a0} val1")
    MessageFormatter.format("''{a0} {a1}", alist) should be("\\val0 val1")
    MessageFormatter.format("H'{'a0'}' {a1}", alist) should be("H{a0} val1")
  }
  */
}