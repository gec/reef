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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.proto.FEP.FrontEndProcessor
import org.totalgrid.reef.models.{ ApplicationInstance, CommunicationProtocolApplicationInstance, ApplicationSchema }
import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.services.ProtoRoutingKeys

// implicits
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.OptionalProtos._
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging.ProtoSerializer._

class FrontEndProcessorService(protected val modelTrans: ServiceTransactable[FrontEndProcessorServiceModel])
    extends BasicProtoService[FrontEndProcessor, ApplicationInstance, FrontEndProcessorServiceModel] {

  override val descriptor = Descriptors.frontEndProcessor
}

class FrontEndProcessorModelFactory(
  pub: ServiceEventPublishers,
  fepModelFac: ModelFactory[CommunicationEndpointConnectionServiceModel])
    extends BasicModelFactory[FrontEndProcessor, FrontEndProcessorServiceModel](pub, classOf[FrontEndProcessor]) {

  def model = new FrontEndProcessorServiceModel(subHandler, fepModelFac.model)
}

class FrontEndProcessorServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  fepModel: CommunicationEndpointConnectionServiceModel)
    extends SquerylServiceModel[FrontEndProcessor, ApplicationInstance]
    with EventedServiceModel[FrontEndProcessor, ApplicationInstance]
    with FrontEndProcessorConversion {

  link(fepModel)

  override def createFromProto(req: FrontEndProcessor): ApplicationInstance = {
    val appInstance = table.where(a => a.id === req.getAppConfig.getUid.toLong).single
    req.getProtocolsList.toList.foreach(p => ApplicationSchema.protocols.insert(new CommunicationProtocolApplicationInstance(p, appInstance.id)))
    info { "Added FEP: " + appInstance.instanceName + " protocols: " + req.getProtocolsList.toList }
    fepModel.onAppChanged(appInstance, true)
    appInstance
  }

  override def updateFromProto(req: FrontEndProcessor, existing: ApplicationInstance): (ApplicationInstance, Boolean) = {
    ApplicationSchema.protocols.delete(ApplicationSchema.protocols.where(p => p.applicationId === existing.id))
    req.getProtocolsList.toList.foreach(p => ApplicationSchema.protocols.insert(new CommunicationProtocolApplicationInstance(p, existing.id)))
    info { "Updated FEP: " + existing.instanceName + " protocols: " + req.getProtocolsList.toList }
    fepModel.onAppChanged(existing, true)
    (existing, true)
  }

  override def preDelete(sql: ApplicationInstance) {
    fepModel.onAppChanged(sql, false)
  }

}

trait FrontEndProcessorConversion
    extends MessageModelConversion[FrontEndProcessor, ApplicationInstance]
    with UniqueAndSearchQueryable[FrontEndProcessor, ApplicationInstance] {

  val table = ApplicationSchema.apps

  def getRoutingKey(req: FrontEndProcessor) = ProtoRoutingKeys.generateRoutingKey {
    req.uid :: Nil
  }

  def searchQuery(proto: FrontEndProcessor, sql: ApplicationInstance) = {
    val protocol = if (proto.getProtocolsCount == 1) Some(proto.getProtocols(1)) else None
    protocol.map(p => sql.id in from(ApplicationSchema.protocols)(t => where(t.protocol === p) select (&(t.applicationId)))) ::
      ApplicationConfigConversion.searchQuery(proto.getAppConfig, sql)
  }

  def uniqueQuery(proto: FrontEndProcessor, sql: ApplicationInstance) = {
    proto.uid.asParam(sql.id === _.toLong) ::
      proto.appConfig.instanceName.asParam(sql.instanceName === _) ::
      Nil
  }

  def isModified(entry: ApplicationInstance, existing: ApplicationInstance): Boolean = {
    true
  }

  def createModelEntry(proto: FrontEndProcessor): ApplicationInstance = {
    ApplicationConfigConversion.createModelEntry(proto.getAppConfig)
  }

  def convertToProto(entry: ApplicationInstance): FrontEndProcessor = {
    val protocols = ApplicationSchema.protocols.where(i => i.applicationId === entry.id).map(_.protocol)

    val b = FrontEndProcessor.newBuilder
      .setUid(entry.id.toString)
      .setAppConfig(ApplicationConfigConversion.convertToProto(entry))

    protocols.foreach(b.addProtocols(_))
    b.build
  }
}
