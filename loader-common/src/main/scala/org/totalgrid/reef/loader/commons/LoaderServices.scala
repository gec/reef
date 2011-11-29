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
package org.totalgrid.reef.loader.commons

import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.ApiBase

import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.clientapi.sapi.client.Promise
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.clientapi.{ Client => JClient }
import org.totalgrid.reef.clientapi.sapi.client.rest.{ RpcProvider, Client }

trait LoaderServices extends AllScadaService {
  def addEquipment(entity: Entity): Promise[Entity]
  def addPoint(point: Point): Promise[Point]
  def addCommand(cmd: Command): Promise[Command]
  def addConfigFile(eq: ConfigFile): Promise[ConfigFile]
  def addEndpoint(eq: Endpoint): Promise[Endpoint]
  def addChannel(eq: CommChannel): Promise[CommChannel]
  def addEdge(obj: EntityEdge): Promise[EntityEdge]
  def get[A <: AnyRef](obj: A): Promise[A]
  def put[A <: AnyRef](obj: A): Promise[A]
  def delete[A <: AnyRef](obj: A): Promise[A]

  def getFeedbackCommands(uuid: ReefUUID): Promise[List[Entity]]
}

object LoaderClient {
  private val serviceInfo = RpcProvider(new LoaderServicesImpl(_),
    List(
      classOf[LoaderServices]))

  def prepareClient(client: Client) {
    client.addRpcProvider(serviceInfo)
  }

  def prepareClient(client: JClient) {
    client.addRpcProvider(serviceInfo)
  }
}

class LoaderServicesImpl(client: Client) extends ApiBase(client) with LoaderServices with AllScadaServiceImpl {
  def addEquipment(eq: Entity) = ops.operation("Can't add equipment: " + eq.getUuid.getValue + " name: " + eq.getName) {
    _.put(eq).map { _.one }
  }

  def addPoint(point: Point) = ops.operation("Can't add point: " + point.getUuid.getValue + " name: " + point.getName) {
    _.put(point).map { _.one }
  }

  def addCommand(cmd: Command) = ops.operation("Can't add command: " + cmd.getUuid.getValue + " name: " + cmd.getName) {
    _.put(cmd).map { _.one }
  }

  def addConfigFile(eq: ConfigFile) = ops.operation("Can't add configFile: " + eq.getUuid.getValue + " name: " + eq.getName) {
    _.put(eq).map { _.one }
  }

  def addEndpoint(eq: Endpoint) = ops.operation("Can't add endpoint: " + eq.getUuid.getValue + " name: " + eq.getName) {
    _.put(eq).map { _.one }
  }

  def addChannel(eq: CommChannel) = ops.operation("Can't add channel: " + eq.getUuid.getValue + " name: " + eq.getName) {
    _.put(eq).map { _.one }
  }

  def addEdge(obj: EntityEdge) = ops.operation("Can't add edge between: " + obj.getParent.getName + "and: " + obj.getChild.getName + " rel: " + obj.getRelationship) {
    _.put(obj).map { _.one }
  }

  def put[A <: AnyRef](obj: A) = ops.operation("Can't put: " + obj.getClass.getSimpleName + " with data: " + obj) {
    _.put(obj).map { _.one }
  }
  def get[A <: AnyRef](obj: A) = ops.operation("Can't get: " + obj.getClass.getSimpleName + " with data: " + obj) {
    _.get(obj).map { _.one }
  }

  def delete[A <: AnyRef](obj: A) = ops.operation("Can't delete: " + obj.getClass.getSimpleName + " with data: " + obj) {
    _.delete(obj).map { _.one }
  }

  import scala.collection.JavaConversions._

  def getFeedbackCommands(uuid: ReefUUID) = {
    ops.operation("Couldn't get feedback commands for point: " + uuid.getValue) { session =>
      val request = Entity.newBuilder.setUuid(uuid).addRelations(Relationship.newBuilder
        .setDescendantOf(true).setRelationship("feedback").setDistance(1)).build
      session.get(request).map { _.one.map { _.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten.toList } }
    }
  }
}