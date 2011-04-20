package org.totalgrid.reef.api.request.builders

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
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, Measurement }

object MeasurementBatchRequestBuilders {

  def makeBatch(m: Measurement): MeasurementBatch = makeBatchAt(m, System.currentTimeMillis)
  def makeBatch(m: List[Measurement]): MeasurementBatch = makeBatchAt(m: java.util.List[Measurement], System.currentTimeMillis)
  def makeBatch(m: java.util.List[Measurement]): MeasurementBatch = makeBatchAt(m, System.currentTimeMillis)

  def makeBatchAt(m: Measurement, defaultMeasurementTime: Long): MeasurementBatch = MeasurementBatch.newBuilder.addMeas(m).setWallTime(defaultMeasurementTime).build
  def makeBatchAt(m: List[Measurement], defaultMeasurementTime: Long): MeasurementBatch = makeBatchAt(m: java.util.List[Measurement], defaultMeasurementTime)
  def makeBatchAt(m: java.util.List[Measurement], defaultMeasurementTime: Long): MeasurementBatch = MeasurementBatch.newBuilder.addAllMeas(m).setWallTime(defaultMeasurementTime).build

}