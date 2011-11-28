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
package org.totalgrid.reef.shell.proto

import presentation.ConfigFileView
import org.apache.felix.gogo.commands.{ Option => GogoOption, Argument, Command }
import scala.collection.JavaConversions._
import java.io.File

import org.totalgrid.reef.util.IOHelpers

@Command(scope = "configfile", name = "list", description = "Prints all config files")
class ConfigFileListCommand extends ReefCommandSupport {

  def doCommand() = {
    val results = services.getConfigFiles
    ConfigFileView.printTable(results.toList)
  }
}

@Command(scope = "configfile", name = "view", description = "View a config file")
class ConfigFileViewCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Config file name", required = true, multiValued = false)
  private var configFileName: String = null

  def doCommand() = {
    val entry = services.getConfigFileByName(configFileName)
    ConfigFileView.printInspect(entry)
  }
}

@Command(scope = "configfile", name = "download", description = "Download a config file")
class ConfigFileDownloadCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Config file name", required = true, multiValued = false)
  private var configFileName: String = null

  @Argument(index = 1, name = "outputFile", description = "File to create, defaults to configFileName", required = false, multiValued = false)
  private var outputFile: String = null

  def doCommand() = {
    val entry = services.getConfigFileByName(configFileName)
    val dataFile = new File(Option(outputFile).getOrElse(configFileName))
    IOHelpers.writeBinary(dataFile, entry.getFile.toByteArray)
  }
}

@Command(scope = "configfile", name = "upload", description = "Upload a config file. If the file already exists in reef overwrite the data in the file.")
class ConfigFileUploadCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "configFileName", description = "Name for config file in reef, this needs to match what is in configfile:list exactly to replace a file.", required = true, multiValued = false)
  var configFileName: String = null

  @Argument(index = 1, name = "inputFile", description = "Local File name to load data from, defaults to configFileName. Needs to be specified if local filename doesn't match reef configFileName.", required = false, multiValued = false)
  var inputFile: String = null

  @GogoOption(name = "-mimeType", description = "Mime Type of file, not necessary if overwriting config file", required = false, multiValued = false)
  var mimeType: String = null

  @GogoOption(name = "-e", description = "Entity to attach configFile to.", required = false, multiValued = false)
  var entity: String = null

  def doCommand() = {
    val currentFile = Option(services.findConfigFileByName(configFileName))

    val dataFile = new File(Option(inputFile).getOrElse(configFileName))
    val data = IOHelpers.readBinary(dataFile)

    val cf = currentFile match {
      case Some(cf) =>
        Option(entity).map { services.getEntityByName(_).getUuid }.foreach { entUuid =>
          services.addConfigFileUsedByEntity(cf, entUuid)
        }
        if (mimeType == null) mimeType = cf.getMimeType
        services.createConfigFile(configFileName, mimeType, data)
      case None =>
        if (mimeType == null) throw new Exception("Must specify mimeType when uploading new config file")

        Option(entity).map { services.getEntityByName(_).getUuid } match {
          case Some(entUuid) => services.createConfigFile(configFileName, mimeType, data, entUuid)
          case None => services.createConfigFile(configFileName, mimeType, data)
        }
    }
    ConfigFileView.printTable(cf :: Nil)
  }
}