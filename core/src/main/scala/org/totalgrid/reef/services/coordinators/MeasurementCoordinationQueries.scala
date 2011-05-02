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
package org.totalgrid.reef.services.coordinators

import org.totalgrid.reef.models._

import org.squeryl.{ Query, Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.util.Conversion._

trait MeasurementCoordinationQueries {
  def availableApps(processType: String) = {
    from(ApplicationSchema.apps, ApplicationSchema.capabilities)((a, c) =>
      where(c.capability === processType and (a.id === c.applicationId))
        select (a))
  }

  def runningApps(processType: String) = {
    from(availableApps(processType))(a =>
      where(a.id in from(ApplicationSchema.heartbeats)(s =>
        where(s.isOnline === true)
          select (&(s.applicationId))))
        select (a))
  }

  def runningApps() = {
    from(ApplicationSchema.apps)(a =>
      where(a.id in from(ApplicationSchema.heartbeats)(s =>
        where(s.isOnline === true)
          select (&(s.applicationId))))
        select (a))
  }

  def availableFeps(protocol: String) = {
    from(runningApps)(a =>
      where(a.id in from(ApplicationSchema.protocols)(p =>
        where(p.protocol === protocol)
          select (p.applicationId)))
        select (a))
  }

  def getLeastLoadedXXX(possibleApps: Query[ApplicationInstance])(countFun: ApplicationInstance => Long): ApplicationInstance = {
    val sortedApps = possibleApps.map(fep =>
      fep -> countFun(fep)).toList.sortBy(_._2)
    val minCount = sortedApps.head._2
    // sort by name to keep results stable
    val minimalApps = sortedApps.filter(minCount == _._2).sortBy(_._1.instanceName)

    minimalApps.head._1
  }

  def getLeastLoadedFrontEnd(possibleApps: Query[ApplicationInstance]): ApplicationInstance = {
    getLeastLoadedXXX(possibleApps)(fep =>
      from(ApplicationSchema.frontEndAssignments)(a =>
        where(a.applicationId === fep.id)
          compute (count)).toLong)
  }

  def getLeastLoadedMeasProc(possibleApps: Query[ApplicationInstance]): ApplicationInstance = {
    getLeastLoadedXXX(possibleApps)(fep =>
      from(ApplicationSchema.measProcAssignments)(a =>
        where(a.applicationId === fep.id)
          compute (count)).toLong)
  }

  def getFep(e: CommunicationEndpoint): Option[ApplicationInstance] = {

    val possibleFeps = e.port.value match {
      case Some(port) => {
        from(availableFeps(e.protocol))(a =>
          where(a.location === port.location.? or a.network === port.network.?)
            select (a))
      }
      case None => {
        availableFeps(e.protocol)
      }
    }

    if (possibleFeps.size == 0) None
    else Some(getLeastLoadedFrontEnd(possibleFeps))
  }

  def getMeasProc(): Option[ApplicationInstance] = {
    val measProcs = runningApps("Processing")

    if (measProcs.size == 0) None
    else Some(getLeastLoadedMeasProc(measProcs))
  }

}

object MeasurementCoordinationQueries extends MeasurementCoordinationQueries