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
package org.totalgrid.reef.services.framework

/**
 * Hooks/callbacks for service implementations to modify standard REST behavior
 */
trait ServiceHooks { self: ServiceTypes =>

  /**
   * Called before create
   *  @param proto  Create request message
   *  @return       Verified/modified create request message
   */
  protected def preCreate(proto: ProtoType): ProtoType = proto

  /**
   * Called after successful create
   *  @param proto  Created response message
   */
  protected def postCreate(created: ModelType, request: ProtoType): Unit = {}

  /**
   * Called before update
   *  @param proto      Update request message
   *  @param existing   Existing model entry
   *  @return           Verified/modified update request message
   */
  protected def preUpdate(proto: ProtoType, existing: ModelType): ProtoType = proto

  /**
   * Called after successful update
   *  @param proto Updated response message
   */
  protected def postUpdate(updated: ModelType, request: ProtoType): Unit = {}

  /**
   * Called before delete
   *  @param proto    Delete request message
   */
  protected def preDelete(proto: ProtoType): Unit = {}

  /**
   * Called after successful delete
   *  @param proto    Deleted objects
   */
  protected def postDelete(proto: List[ProtoType]): Unit = {}
}