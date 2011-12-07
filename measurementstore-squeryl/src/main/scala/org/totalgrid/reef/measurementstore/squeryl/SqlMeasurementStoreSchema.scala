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
package org.totalgrid.reef.measurementstore.squeryl

import org.squeryl.{ Schema, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

case class Measurement(
    val pointId: Long,
    val measTime: Long,
    val proto: Array[Byte]) extends KeyedEntity[Long] {
  var id: Long = 0
}

case class MeasName(
    val name: String) extends KeyedEntity[Long] {
  var id: Long = 0
}

case class CurrentValue(
    val pointId: Long,
    val proto: Array[Byte]) extends KeyedEntity[Long] {
  var id: Long = pointId
}

object SqlMeasurementStoreSchema extends Schema {
  val updates = table[Measurement]
  val names = table[MeasName]
  val currentValues = table[CurrentValue]

  on(updates)(s => declare(
    columns(s.pointId, s.measTime.~) are (indexed)))

  on(names)(s => declare(
    columns(s.name) are (indexed, unique)))

  def reset() = {
    drop // its protected for some reason
    create
  }

}