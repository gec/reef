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
package org.totalgrid.reef.benchmarks

import org.totalgrid.reef.standalone.InMemoryNode
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

class BenchmarkTestBase extends FunSuite with BeforeAndAfterAll {
  def client = {
    val c = InMemoryNode.connection.login(InMemoryNode.userSettings.getUserName, InMemoryNode.userSettings.getUserPassword).await
    c.setHeaders(c.getHeaders.setTimeout(120000))
    c.setHeaders(c.getHeaders.setResultLimit(10000))

    c
  }

  override def beforeAll {
    InMemoryNode.initialize("../standalone-node.cfg", true, None)
  }
}