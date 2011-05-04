/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.models

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.util.LazyVar

case class FrontEndAssignment(
    val endpointId: Long,
    val state: Int,
    val enabled: Boolean,

    val serviceRoutingKey: Option[String],
    val applicationId: Option[Long],
    var assignedTime: Option[Long],
    var offlineTime: Option[Long],
    var onlineTime: Option[Long]) extends ModelWithId {

  def this() = this(0, 0, false, Some(""), Some(0), Some(0), Some(0), Some(0))

  val application = LazyVar(mayHaveOne(ApplicationSchema.apps, applicationId))
  val endpoint = LazyVar(ApplicationSchema.endpoints.where(p => p.id === endpointId).headOption)

}

case class MeasProcAssignment(
    val endpointId: Long,
    val serviceRoutingKey: Option[String],
    val applicationId: Option[Long],
    var assignedTime: Option[Long],
    var readyTime: Option[Long]) extends ModelWithId {

  def this() = this(0, Some(""), Some(0), Some(0), Some(0))

  val application = LazyVar(mayHaveOne(ApplicationSchema.apps, applicationId))
  val endpoint = LazyVar(ApplicationSchema.endpoints.where(p => p.id === endpointId).headOption)
}

case class CommunicationProtocolApplicationInstance(
    val protocol: String,
    val applicationId: Long) extends ModelWithId {

  val application = LazyVar(hasOne(ApplicationSchema.apps, applicationId))
}

case class ChannelStatus(val name: String, val state: Int) extends ModelWithId