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
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.Command

import org.totalgrid.reef.shell.proto.request.ChannelRequest

//import scala.collection.JavaConversions._

@Command(scope = "channel", name = "list", description = "Prints channel information")
class ChannelListCommand extends ReefCommandSupport {

  def doCommand() = {

    val results = ChannelRequest.getAll(reefSession)
    println("Found " + results.size + " channel(s)")
    results.foreach { channel =>
      println(channel.getName + ": " + channel.getState)
    }
  }
}