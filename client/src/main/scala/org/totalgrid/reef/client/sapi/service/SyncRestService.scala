/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.service

import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Response }

trait HasSyncRestGet extends HasServiceType {
  def get(req: ServiceType, env: BasicRequestHeaders): Response[ServiceType] = RestResponses.noGet[ServiceType]
}

trait HasSyncRestPut extends HasServiceType {
  def put(req: ServiceType, env: BasicRequestHeaders): Response[ServiceType] = RestResponses.noPut[ServiceType]
}

trait HasSyncRestDelete extends HasServiceType {
  def delete(req: ServiceType, env: BasicRequestHeaders): Response[ServiceType] = RestResponses.noDelete[ServiceType]
}

trait HasSyncRestPost extends HasServiceType {
  def post(req: ServiceType, env: BasicRequestHeaders): Response[ServiceType] = RestResponses.noGet[ServiceType]
}

trait SyncRestService extends HasSyncRestGet with HasSyncRestPut with HasSyncRestDelete with HasSyncRestPost