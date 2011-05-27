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
import org.apache.felix.gogo.commands.{ Argument, Command }

import scala.collection.JavaConversions._

@Command(scope = "configfile", name = "list", description = "Prints all config files")
class ConfigFileListCommand extends ReefCommandSupport {

  def doCommand() = {
    val results = services.getAllConfigFiles
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