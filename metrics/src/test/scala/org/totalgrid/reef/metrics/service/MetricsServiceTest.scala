package org.totalgrid.reef.metrics.service

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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.metrics.client.proto.Metrics.MetricsRead
import org.totalgrid.reef.client.sapi.client.{ Response, BasicRequestHeaders }
import org.totalgrid.reef.client.proto.Envelope.Status
import scala.collection.JavaConversions._
import org.totalgrid.reef.metrics.{ SimpleMetricsSink, MappedMetricsHolder, MetricsHooks, MetricsSink }

@RunWith(classOf[JUnitRunner])
class MetricsServiceTest extends FunSuite with ShouldMatchers {

  class MockMetricsSink extends MappedMetricsHolder[SimpleMetricsSink] {
    def newHolder(name: String) = new SimpleMetricsSink
    def getInstance(name: String) = getStore(name)
  }

  class Fixture {
    val metrics = new MockMetricsSink
    val srv = new MetricsService(metrics)

    def get(req: MetricsRead): Response[MetricsRead] = {
      var resp: Option[Response[MetricsRead]] = None
      srv.getAsync(req, BasicRequestHeaders.empty) { r: Response[MetricsRead] =>
        resp = Some(r)
      }
      resp.get
    }

    def delete(req: MetricsRead): Response[MetricsRead] = {
      var resp: Option[Response[MetricsRead]] = None
      srv.deleteAsync(req, BasicRequestHeaders.empty) { r: Response[MetricsRead] =>
        resp = Some(r)
      }
      resp.get
    }

    def getFun(store: String, hook: String, typ: MetricsHooks.HookType) = {
      metrics.getStore("testStore").getStore(store).getSinkFunction(hook, typ)
    }

    def getAndCheck(name: String, v: Double) = {
      val result = get(MetricsRead.newBuilder.addFilters(name).build).expectOne(Status.OK)
      val l = result.getResultsList.toList
      l.size should equal(1)
      l.head.getName should equal(name)
      l.head.getValue should equal(v)
      result.hasReadTime should equal(true)
    }
    def getAllAndCheckOne(name: String, v: Double) = {
      val result = get(MetricsRead.newBuilder.build).expectOne(Status.OK)
      val l = result.getResultsList.toList
      l.size should equal(1)
      l.head.getName should equal(name)
      l.head.getValue should equal(v)
      result.hasReadTime should equal(true)
    }
  }

  test("Get counter") {
    val f = new Fixture
    f.getFun("lowerStore", "hook01", MetricsHooks.Counter)(5)

    val result = f.get(MetricsRead.newBuilder.build).expectOne(Status.OK)
    val l = result.getResultsList.toList
    l.size should equal(1)
    l.head.getName should equal("lowerStore.hook01")
    l.head.getValue should equal(5.0)
    result.hasReadTime should equal(true)
  }

  test("Get average") {
    val f = new Fixture
    val fun = f.getFun("lowerStore", "hook01", MetricsHooks.Average)
    fun(5)
    fun(6)

    val result = f.get(MetricsRead.newBuilder.build).expectOne(Status.OK)
    val l = result.getResultsList.toList
    l.size should equal(1)
    l.head.getName should equal("lowerStore.hook01")
    l.head.getValue should equal(5.5)
  }

  test("Get multiple") {
    val f = new Fixture
    f.getFun("lowerStore", "hook01", MetricsHooks.Counter)(5)
    f.getFun("lowerStore", "hook02", MetricsHooks.Counter)(8)

    val result = f.get(MetricsRead.newBuilder.build).expectOne(Status.OK)
    val l = result.getResultsList.toList
    l.size should equal(2)
    l.find(_.getName == "lowerStore.hook01").get.getValue should equal(5.0)
    l.find(_.getName == "lowerStore.hook02").get.getValue should equal(8.0)
  }

  test("Get multiple filtered") {
    val f = new Fixture
    f.getFun("lowerStore", "hook01", MetricsHooks.Counter)(5)
    f.getFun("lowerStore", "hook02", MetricsHooks.Counter)(8)

    val result = f.get(MetricsRead.newBuilder.addFilters("lowerStore.hook02").build).expectOne(Status.OK)
    val l = result.getResultsList.toList
    l.size should equal(1)
    //l.find(_.getName == "lowerStore.hook01").get.getValue should equal(5.0)
    l.find(_.getName == "lowerStore.hook02").get.getValue should equal(8.0)
  }

  test("Delete no params error") {
    val f = new Fixture
    f.getFun("lowerStore", "hook01", MetricsHooks.Counter)(5)
    f.delete(MetricsRead.newBuilder.build).expectNone(Status.BAD_REQUEST)
  }

  test("Delete single") {
    val f = new Fixture
    f.getFun("lowerStore", "hook01", MetricsHooks.Counter)(5)

    f.getAllAndCheckOne("lowerStore.hook01", 5.0)

    val result = f.delete(MetricsRead.newBuilder.addFilters("*").build).expectOne(Status.DELETED)
    val l = result.getResultsList.toList
    l.size should equal(1)
    l.head.getName should equal("lowerStore.hook01")
    l.head.getValue should equal(0.0)
    result.hasReadTime should equal(true)

  }

  test("Delete single with filter") {
    val f = new Fixture
    f.getFun("lowerStore", "hook01", MetricsHooks.Counter)(5)
    f.getFun("lowerStore", "hook02", MetricsHooks.Counter)(8)

    val result = f.delete(MetricsRead.newBuilder.addFilters("lowerStore.hook02").build).expectOne(Status.DELETED)
    val l = result.getResultsList.toList
    l.size should equal(1)
    l.head.getName should equal("lowerStore.hook02")
    l.head.getValue should equal(0.0)
    result.hasReadTime should equal(true)

    f.getAndCheck("lowerStore.hook01", 5.0)
  }

}