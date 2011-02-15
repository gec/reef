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

import org.totalgrid.reef.api.ServiceTypes.Response

import org.totalgrid.reef.messaging.ServiceEndpoint; import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.services.ServiceProviderHeaders._

import org.totalgrid.reef.proto.Model.ConfigFile
import org.totalgrid.reef.api._

class ConfigService(protected val entityService: EntityService, protected val entityEdgetService: EntityEdgeService)
    extends ServiceEndpoint[ConfigFile] {

  override val descriptor = Descriptors.configFile

  override def delete(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("delete")
  override def post(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("post")
  override def get(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = noVerb("get")

  override def put(req: ConfigFile, env: RequestEnv): Response[ConfigFile] = {

    env.subQueue.foreach(queueName => throw new ServiceException("Subscribe not allowed: " + queueName))

    //  TODO: load a file by name or, even better, use getFile to get the bytes of the file
    if (req.hasName) {
      //LoadManager.load( client??, req.getName)
    }

    new Response(Envelope.Status.OK, req)
  }
}
