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

import org.totalgrid.reef.proto.Model.{ Entity, ConfigFile }
import org.totalgrid.reef.api.ReefServiceException

/**
 * Non-exhaustive API for using the reef Config File service, not all valid permutations are reflected here.
 * Additional functions are expected to be added by clients who extends this interface and add the needed
 * functionality using ConfigFileHelpersImpl as a examples of other valid queries. Note that this class is a
 * simple interface so it should be easily mockable in test code. Note also that when are using Lists etc. we
 * are using the java classes instead of scala versions b/c its easier to use java lists in scala than scala
 * lists in java.
 *
 * Config files are for larger hunks of opaque data for use by external applications. Config files can be
 * used by 0, 1 or many entities. Config files can be searched for by name, uid or by entities they are
 * related to. Names must be unique system-wide. Searches can all be filtered by mimeType, which can be
 * helpful is name is unknown.
 */
trait ConfigFileHelpers {
  /**
   * retrieve a config file by its UID
   */
  @throws(classOf[ReefServiceException])
  def getConfigFileByUid(uid: String): ConfigFile
  /**
   * retrieve a config file by its name
   */
  @throws(classOf[ReefServiceException])
  def getConfigFileByName(name: String): ConfigFile

  /**
   * search for all config files "used" by an entity
   */
  @throws(classOf[ReefServiceException])
  def getConfigFilesUsedByEntity(entity: Entity): java.util.List[ConfigFile]
  /**
   * search for all config files "used" by an entity
   */
  @throws(classOf[ReefServiceException])
  def getConfigFilesUsedByEntityUid(entityUid: String): java.util.List[ConfigFile]
  /**
   * search for all config files "used" by an entity
   */
  @throws(classOf[ReefServiceException])
  def getConfigFilesUsedByEntityName(entityName: String): java.util.List[ConfigFile]

  /**
   * search for all config files "used" by an entity, only returns files with matching mimeType
   */
  @throws(classOf[ReefServiceException])
  def getConfigFilesUsedByEntity(entity: Entity, mimeType: String): java.util.List[ConfigFile]

  /**
   * search for all config files "used" by an entity, only returns files with matching mimeType
   */
  @throws(classOf[ReefServiceException])
  def getConfigFilesUsedByEntityUid(entityUid: String, mimeType: String): java.util.List[ConfigFile]
  /**
   * search for all config files "used" by an entity, only returns files with matching mimeType
   */
  @throws(classOf[ReefServiceException])
  def getConfigFilesUsedByEntityName(entityName: String, mimeType: String): java.util.List[ConfigFile]

  /**
   * create a "free-floating" ConfigFile that isnt "used" by any entities. This means is only retrievable
   * by name or uid (or mimeType if there is only one file with that type in system)
   */
  @throws(classOf[ReefServiceException])
  def createConfigFile(name: String, mimeType: String, data: Array[Byte]): ConfigFile
  /**
   * create a ConfigFile that is "used" by an Entity, it is now queryable by name, mimeType and entity.
   */
  @throws(classOf[ReefServiceException])
  def createConfigFile(name: String, mimeType: String, data: Array[Byte], entity: Entity): ConfigFile
  /**
   * create a ConfigFile that is "used" by an Entity, it is now queryable by name, mimeType and entity.
   */
  @throws(classOf[ReefServiceException])
  def createConfigFile(name: String, mimeType: String, data: Array[Byte], entityUid: String): ConfigFile

  /**
   * update the text of the previously retrieved ConfigFile
   */
  @throws(classOf[ReefServiceException])
  def updateConfigFile(configFile: ConfigFile, data: Array[Byte]): ConfigFile

  /**
   * adds another Entity as a "user" of the ConfigFile
   */
  @throws(classOf[ReefServiceException])
  def addConfigFileUserByEntity(configFile: ConfigFile, entity: Entity): ConfigFile

  /**
   * delete the passed in config file and all "using" relationships to Entities
   */
  @throws(classOf[ReefServiceException])
  def deleteConfigFile(configFile: ConfigFile): ConfigFile
}

