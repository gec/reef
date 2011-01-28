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

import org.totalgrid.reef.models.{ ApplicationSchema }
import org.totalgrid.reef.messaging.{ ProtoServiceable, ProtoServiceException, RequestEnv }
import org.totalgrid.reef.messaging.ProtoServiceTypes._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoServiceEndpoint
import org.totalgrid.reef.services.{ ServiceEventPublishers, ServiceSubscriptionHandler }

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._
//import org.squeryl.Table
//import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }
import org.totalgrid.reef.util.{ Logging, XMLHelper }
import org.totalgrid.reef.services.ProtoRoutingKeys

import OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._

import org.totalgrid.reef.services.ServiceProviderHeaders._

//import org.squeryl.dsl.ast.{ LogicalBoolean, BinaryOperatorNodeLogicalBoolean }

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.loader.equipment.EquipmentModel
import org.totalgrid.reef.loader.communications.CommunicationsModel
import org.totalgrid.reef.proto.Model.{ ConfigFile, Entity, Relationship, EntityEdge, Point, Command }
//import org.totalgrid.reef.services.core.{ EntityService, EntityEdgeService }

class ConfigService(protected val entityService: EntityService, protected val entityEdgetService: EntityEdgeService)
    extends ProtoServiceable[ConfigFile] with ProtoServiceEndpoint {

  def deserialize(bytes: Array[Byte]) = ConfigFile.parseFrom(bytes)
  val servedProto: Class[_] = classOf[ConfigFile]

  //override def put(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("put")
  override def delete(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("delete")
  override def post(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("post")
  override def get(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("get")

  override def put(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = {

    env.subQueue.foreach(queueName => throw new ProtoServiceException("Subscribe not allowed: " + queueName))

    //  TODO: load a file by name or, even better, use getFile to get the bytes of the file
    if (req.hasName) {
      //LoadManager.load( client??, req.getName)
    }

    new Response(Envelope.Status.OK, req)
  }
}
