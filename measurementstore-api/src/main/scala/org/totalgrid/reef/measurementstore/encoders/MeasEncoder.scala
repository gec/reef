/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.measurementstore.encoders

import org.totalgrid.reef.api.proto.Measurements

trait MeasEncoder {

  /**
   * Turns a sequence of measurements into a MeasArchive, applying an encoding strategy
   * @param meas Sequence of measurements in ascending time order
   * @return MeasArchive object representing the sequence
   */
  def encode(meas: Seq[Measurements.Measurement]): Array[Byte]

  /**
   * Turns an archive into a sequence of measurements, reversing the encoding strategy      @param meas Sequence of measurements in ascending time order      @return ascending time ordered sequence of measurements
   */
  def decode(serialized: Array[Byte]): Seq[Measurements.Measurement]

  /// Determines the type for a MeasStorageUnit    
  def getType(last: Measurements.MeasArchiveUnit): Option[Measurements.Measurement.Type] = {
    if (last.hasIntVal) Some(Measurements.Measurement.Type.INT)
    else if (last.hasBoolVal) Some(Measurements.Measurement.Type.BOOL)
    else if (last.hasDoubleVal) Some(Measurements.Measurement.Type.DOUBLE)
    else if (last.hasStringVal) Some(Measurements.Measurement.Type.STRING)
    else None
  }

}
