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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor
import org.totalgrid.reef.models.{ ApplicationInstance, CommunicationProtocolApplicationInstance, ApplicationSchema }
import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.services
import java.util.UUID
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinator }

// implicits
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._
import scala.collection.JavaConversions._
import org.totalgrid.reef.models.UUIDConversions._
import org.totalgrid.reef.services.framework.ProtoSerializer._

class FrontEndProcessorService(protected val model: FrontEndProcessorServiceModel)
    extends SyncModeledServiceBase[FrontEndProcessor, ApplicationInstance, FrontEndProcessorServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.frontEndProcessor
}

class FrontEndProcessorServiceModel(
  coordinator: MeasurementStreamCoordinator)
    extends SquerylServiceModel[Long, FrontEndProcessor, ApplicationInstance]
    with EventedServiceModel[FrontEndProcessor, ApplicationInstance]
    with FrontEndProcessorConversion {

  override def createFromProto(context: RequestContext, req: FrontEndProcessor): ApplicationInstance = {
    val appInstance = table.where(a => a.entityId === UUID.fromString(req.getAppConfig.getUuid.getValue)).single
    req.getProtocolsList.toList.foreach(p => ApplicationSchema.protocols.insert(new CommunicationProtocolApplicationInstance(p, appInstance.id)))
    logger.info("Added FEP: " + appInstance.instanceName + " protocols: " + req.getProtocolsList.toList)
    coordinator.onFepAppChanged(context, appInstance, true)
    appInstance
  }

  override def updateFromProto(context: RequestContext, req: FrontEndProcessor, existing: ApplicationInstance): (ApplicationInstance, Boolean) = {
    ApplicationSchema.protocols.delete(ApplicationSchema.protocols.where(p => p.applicationId === existing.id))
    req.getProtocolsList.toList.foreach(p => ApplicationSchema.protocols.insert(new CommunicationProtocolApplicationInstance(p, existing.id)))
    logger.info("Updated FEP: " + existing.instanceName + " protocols: " + req.getProtocolsList.toList)
    coordinator.onFepAppChanged(context, existing, true)
    (existing, true)
  }

  override def preDelete(context: RequestContext, sql: ApplicationInstance) {
    coordinator.onFepAppChanged(context, sql, false)
  }

}

trait FrontEndProcessorConversion
    extends UniqueAndSearchQueryable[FrontEndProcessor, ApplicationInstance] {

  val table = ApplicationSchema.apps

  def sortResults(list: List[FrontEndProcessor]) = list.sortBy(_.getAppConfig.getInstanceName)

  def getRoutingKey(req: FrontEndProcessor) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.value :: Nil
  }

  def relatedEntities(entries: List[ApplicationInstance]) = {
    entries.map { _.entityId }
  }

  def searchQuery(proto: FrontEndProcessor, sql: ApplicationInstance) = {
    val protocol = if (proto.getProtocolsCount == 1) Some(proto.getProtocols(1)) else None
    protocol.map(p => sql.id in from(ApplicationSchema.protocols)(t => where(t.protocol === p) select (&(t.applicationId)))) ::
      ApplicationConfigConversion.searchQuery(proto.getAppConfig, sql)
  }

  def uniqueQuery(proto: FrontEndProcessor, sql: ApplicationInstance) = {
    proto.uuid.value.asParam(sql.id === _.toLong) ::
      proto.appConfig.instanceName.asParam(sql.instanceName === _) ::
      Nil
  }

  def isModified(entry: ApplicationInstance, existing: ApplicationInstance): Boolean = {
    true
  }

  def convertToProto(entry: ApplicationInstance): FrontEndProcessor = {
    val protocols = ApplicationSchema.protocols.where(i => i.applicationId === entry.id).map(_.protocol)

    val b = FrontEndProcessor.newBuilder
      .setUuid(makeUuid(entry))
      .setAppConfig(ApplicationConfigConversion.convertToProto(entry))

    protocols.foreach(b.addProtocols(_))
    b.build
  }
}
