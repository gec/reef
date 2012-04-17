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
package org.totalgrid.reef.metrics.client.impl

import org.totalgrid.reef.metrics.client.MetricsService
import org.totalgrid.reef.metrics.client.proto.Metrics.MetricsRead
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.sapi.client.rpc.framework.ApiBase

class MetricsServiceImpl(client: Client) extends ApiBase(client) with MetricsService {

  def getMetrics(): MetricsRead = {
    ops.operation("getMetrics failed") { rest =>
      rest.get(MetricsRead.newBuilder.build).map(_.one)
    }.await
  }

  def getMetricsWithFilter(filter: String): MetricsRead = {
    ops.operation("getMetrics failed") { rest =>
      rest.get(MetricsRead.newBuilder.addFilters(filter).build).map(_.one)
    }.await
    //client.get(MetricsRead.newBuilder.addFilters(filter).build).await.expectOne
  }

  def getMetricsWithFilters(filters: java.util.List[String]): MetricsRead = {
    ops.operation("getMetrics failed") { rest =>
      rest.get(MetricsRead.newBuilder.addAllFilters(filters).build).map(_.one)
    }.await
    //client.get(MetricsRead.newBuilder.addAllFilters(filters).build).await.expectOne
  }

  def resetMetrics(): MetricsRead = {
    ops.operation("getMetrics failed") { rest =>
      rest.delete(MetricsRead.newBuilder.addFilters("*").build).map(_.one)
    }.await
    //client.delete(MetricsRead.newBuilder.addFilters("*").build).await.expectOne
  }

  def resetMetricsWithFilter(filter: String): MetricsRead = {
    ops.operation("getMetrics failed") { rest =>
      rest.delete(MetricsRead.newBuilder.addFilters(filter).build).map(_.one)
    }.await
    //client.delete(MetricsRead.newBuilder.addFilters(filter).build).await.expectOne
  }

  def resetMetricsWithFilters(filters: java.util.List[String]): MetricsRead = {
    ops.operation("getMetrics failed") { rest =>
      rest.delete(MetricsRead.newBuilder.addAllFilters(filters).build).map(_.one)
    }.await
    //client.delete(MetricsRead.newBuilder.addAllFilters(filters).build).await.expectOne
  }
}