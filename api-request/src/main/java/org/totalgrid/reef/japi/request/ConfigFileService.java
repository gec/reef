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
package org.totalgrid.reef.japi.request;

import java.util.List;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.proto.Model.ConfigFile;
import org.totalgrid.reef.proto.Model.ReefUUID;

/**
 * Non-exhaustive API for using the reef Config File service, not all valid permutations are reflected here.
 * Additional functions are expected to be added by clients who extends this interface and add the needed
 * functionality using ConfigFileServiceImpl as a examples of other valid queries. Note that this class is a
 * simple interface so it should be easily mockable in test code. Note also that when are using Lists etc. we
 * are using the java classes instead of scala versions b/c its easier to use java lists in scala than scala
 * lists in java.
 * <p/>
 * Config files are for larger hunks of opaque data for use by external applications. Config files can be
 * used by 0, 1 or many entities. Config files can be searched for by name, uid or by entities they are
 * related to. Names must be unique system-wide. Searches can all be filtered by mimeType, which can be
 * helpful is name is unknown.
 */
public interface ConfigFileService
{
    /**
     * Get all config files
     */
    List<ConfigFile> getAllConfigFiles() throws ReefServiceException;

    /**
     * retrieve a config file by its UID
     */
    ConfigFile getConfigFileByUid( ReefUUID uid ) throws ReefServiceException;

    /**
     * retrieve a config file by its name
     */
    ConfigFile getConfigFileByName( String name ) throws ReefServiceException;

    /**
     * search for all config files "used" by an entity
     */
    List<ConfigFile> getConfigFilesUsedByEntity( ReefUUID entityUid ) throws ReefServiceException;

    /**
     * search for all config files "used" by an entity, only returns files with matching mimeType
     */
    List<ConfigFile> getConfigFilesUsedByEntity( ReefUUID entityUid, String mimeType ) throws ReefServiceException;

    /**
     * create a "free-floating" ConfigFile that isnt "used" by any entities. This means is only retrievable
     * by name or uid (or mimeType if there is only one file with that type in system)
     */
    ConfigFile createConfigFile( String name, String mimeType, byte[] data ) throws ReefServiceException;

    /**
     * create a ConfigFile that is "used" by an Entity, it is now queryable by name, mimeType and entity.
     */
    ConfigFile createConfigFile( String name, String mimeType, byte[] data, ReefUUID entityUid ) throws ReefServiceException;

    /**
     * update the text of the previously retrieved ConfigFile
     */
    ConfigFile updateConfigFile( ConfigFile configFile, byte[] data ) throws ReefServiceException;

    /**
     * adds another Entity as a "user" of the ConfigFile
     */
    ConfigFile addConfigFileUserByEntity( ConfigFile configFile, ReefUUID entityUid ) throws ReefServiceException;

    /**
     * delete the passed in config file and all "using" relationships to Entities
     */
    ConfigFile deleteConfigFile( ConfigFile configFile );
}
