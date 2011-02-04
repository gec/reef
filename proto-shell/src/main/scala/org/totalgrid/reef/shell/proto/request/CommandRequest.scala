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
package org.totalgrid.reef.shell.proto.request

import org.totalgrid.reef.proto.Commands.{ UserCommandRequest, CommandAccess, CommandRequest => ProtoCommandRequest }
import org.totalgrid.reef.proto.Model.Command
import org.totalgrid.reef.protoapi.client.SyncServiceClient

object CommandRequest {

  def blockCommands(ids: List[String], user: String, client: SyncServiceClient) = {
    val names = ids.map(getNameFromId(_, client))
    val block = CommandAccess.newBuilder
      .setAccess(CommandAccess.AccessMode.BLOCKED)
      .setUser(user)
    names.foreach(block.addCommands(_))
    client.put_one(block.build)
  }

  def removeSelects(uids: List[String], user: String, client: SyncServiceClient) = {
    uids.map { uid =>
      client.delete_one(accessEntry(uid, user))
    }
  }

  def statusOf(id: String, client: SyncServiceClient) = {
    client.get_one(requestForUid(id))
  }

  def getNameFromId(id: String, client: SyncServiceClient) = {
    if (isEntityUid(id)) {
      client.get_one(forEntityUid(id)).getName
    } else id
  }

  def issueForId(id: String, user: String, client: SyncServiceClient) = {
    issue(getNameFromId(id, client), user, client)
  }

  def issue(name: String, user: String, client: SyncServiceClient) = {
    val access = client.put_one(makeSelect(name, user))
    val result = client.put_one(makeRequest(name, user))
    client.delete_one(access)
    result
  }

  def getAllAccessEntries(client: SyncServiceClient) = {
    client.get(allAccessEntries)
  }
  def getAccessEntry(id: String, client: SyncServiceClient) = {
    client.get_one(accessEntry(id))
  }

  def allAccessEntries = CommandAccess.newBuilder.setUid("*").build
  def accessEntry(uid: String) = CommandAccess.newBuilder.setUid(uid).build
  def accessEntry(uid: String, user: String) = CommandAccess.newBuilder.setUid(uid).setUser(user).build

  def requestForUid(uid: String) = UserCommandRequest.newBuilder.setUid(uid).build

  def isEntityUid(id: String): Boolean = {
    try {
      Integer.parseInt(id.trim)
      true
    } catch {
      case ex: NumberFormatException => false
    }
  }

  def forEntityUid(uid: String) = {
    Command.newBuilder.setEntity(EntityRequest.forId(uid)).build
  }

  def makeSelect(name: String, user: String) =
    CommandAccess.newBuilder.addCommands(name).setAccess(CommandAccess.AccessMode.ALLOWED).setUser(user).build

  def makeRequest(name: String, user: String) = {
    UserCommandRequest.newBuilder.setCommandRequest(
      ProtoCommandRequest.newBuilder.setName(name)).setUser(user).build
  }
}