/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Model.{ Entity, EntityAttributes }
import org.totalgrid.reef.api.ReefServiceException

trait EntityService {


  /**
   * Get an entity using its unique identification.
   * @param uid   The entity id.
   * @return The entity object.
   */
  @throws(classOf[ReefServiceException])
  def getEntityByUid(uid: ReefUUID): Entity

  /**
   * Get an entity using its name.
   * @param name   The configured name of the entity.
   * @return The entity object.
   */
  @throws(classOf[ReefServiceException])
  def getEntityByName(name: String): Entity

  /**
   * Find all entities with a specified type.
   * @param typ   The entity type to search for.
   * @return The list of entities that have the specified type.
   */
  @throws(classOf[ReefServiceException])
  def getAllEntitiesWithType(typ: String): java.util.List[Entity]

  /**
   * Get the attributes for a specified Entity.
   * @param uid   The entity id.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def getEntityAttributes(uid: ReefUUID): EntityAttributes


  /**
   * Remove a specific attribute by name for a particular Entity.
   * @param uid   The entity id.
   * @param attrName    The name of the attribute.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def removeEntityAttribute(uid: ReefUUID, attrName: String): EntityAttributes


  /**
   * Clear all attributes for a specified Entity.
   * @param uid   The entity id.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def clearEntityAttributes(uid: ReefUUID): EntityAttributes


  /**
   * Set a boolean attribute by name for a specified Entity.
   * @param uid     The entity id.
   * @param name    The name of the attribute.
   * @param value   The attribute value.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def setEntityAttribute(uid: ReefUUID, name: String, value: Boolean): EntityAttributes

  /**
   * Set a signed 64-bit integer attribute by name for a specified Entity.
   * @param uid     The entity id.
   * @param name    The name of the attribute.
   * @param value   The attribute value.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def setEntityAttribute(uid: ReefUUID, name: String, value: Long): EntityAttributes

  /**
   * Set an attribute of type double by name for a specified Entity.
   * @param uid     The entity id.
   * @param name    The name of the attribute.
   * @param value   The attribute value.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def setEntityAttribute(uid: ReefUUID, name: String, value: Double): EntityAttributes

  /**
   * Set a string attribute by name for a specified Entity.
   * @param uid     The entity id.
   * @param name    The name of the attribute.
   * @param value   The attribute value.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def setEntityAttribute(uid: ReefUUID, name: String, value: String): EntityAttributes

  /**
   * Set an attribute of type Array<Byte> by name for a specified Entity.
   * @param uid     The entity id.
   * @param name    The name of the attribute.
   * @param value   The attribute value.
   * @return The entity and its associated attributes.
   */
  @throws(classOf[ReefServiceException])
  def setEntityAttribute(uid: ReefUUID, name: String, value: Array[Byte]): EntityAttributes
}