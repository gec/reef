/**
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
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.util.Logging
//import org.totalgrid.reef.protocol.dnp3.{ IDataObserver, Binary, Analog, Counter, ControlStatus, SetpointStatus }

import org.totalgrid.reef.proto.Mapping
import org.totalgrid.reef.proto.Measurements.{ Measurement => Meas, MeasurementBatch => MeasBatch }

/** Transforms dnp3 values as they come in from the stack and forwards them.
 *	@param cfg Measurement mapping configuration
 * 	@param accept Function that accepts the converted measurement types
 */
class MeasAdapter(cfg: Mapping.IndexMapping, accept: MeasBatch => Unit) extends IDataObserver with Logging {

  val map = MapGenerator.getMeasMap(cfg)
  var batch = MeasBatch.newBuilder

  override def _Start() =
    batch = MeasBatch.newBuilder.setWallTime(System.currentTimeMillis)

  override def _End() = if (batch.getMeasCount > 0) {
    info("Publishing batch size: " + batch.getMeasCount)
    accept(batch.build)
  }

  override def _Update(v: Binary, index: Long): Unit =
    add(index, Mapping.DataType.BINARY) { DNPTranslator.translate(v, _) }

  override def _Update(v: Analog, index: Long) =
    add(index, Mapping.DataType.ANALOG) { DNPTranslator.translate(v, _) }

  override def _Update(v: Counter, index: Long) =
    add(index, Mapping.DataType.COUNTER) { DNPTranslator.translate(v, _) }

  override def _Update(v: SetpointStatus, index: Long) =
    add(index, Mapping.DataType.SETPOINT_STATUS) { DNPTranslator.translate(v, _) }

  override def _Update(v: ControlStatus, index: Long) =
    add(index, Mapping.DataType.CONTROL_STATUS) { DNPTranslator.translate(v, _) }

  /// if the measurement exits, transform using the specified function and send to the actor
  private def add(index: Long, t: Mapping.DataType)(f: String => Meas) = {
    map.get((index, t.getNumber)) match {
      case Some(name) => batch.addMeas(f(name))
      case None => debug { "Unknown type/index: " + t.toString + "/" + index }
    }
  }
}