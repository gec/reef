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
package org.totalgrid.reef.api.protocol.dnp3.slave

import scala.collection.JavaConversions._

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.api.proto.Measurements.{ Measurement }
import org.totalgrid.reef.api.protocol.dnp3._
import org.totalgrid.reef.api.proto.Mapping.{ DataType, IndexMapping, MeasMap }

class DataObserverPublisher(mappingProto: IndexMapping, dataObserver: IDataObserver) extends Logging {

  private val mapping = mappingProto.getMeasmapList.toList.map { e =>
    e.getPointName -> e
  }.toMap

  def publishMeasurements(measurements: List[Measurement]) {
    inTransaction {
      measurements.foreach { meas =>
        mapping.get(meas.getName) match {
          case Some(measMap) =>
            updateSingleMeasurement(meas, measMap)
          case None =>
            logger.info("Got unknown measurement: " + meas.getName)
        }
      }
    }
  }

  private def updateSingleMeasurement(meas: Measurement, measMap: MeasMap) {
    import ProtoToDnpMeasurementTranslator._
    measMap.getType match {
      case DataType.ANALOG => dataObserver.Update(getAnalog(meas), measMap.getIndex)
      case DataType.BINARY => dataObserver.Update(getBinary(meas), measMap.getIndex)
      case DataType.COUNTER => dataObserver.Update(getCounter(meas), measMap.getIndex)
      case DataType.CONTROL_STATUS => dataObserver.Update(getControlStatus(meas), measMap.getIndex)
      case DataType.SETPOINT_STATUS => dataObserver.Update(getSetpointStatus(meas), measMap.getIndex)
    }
  }

  private def inTransaction(func: => Unit) {
    val transaction = new Transaction
    transaction.Start(dataObserver)
    try {
      func
    } finally {
      transaction.End()
    }
  }

}