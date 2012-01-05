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
package org.totalgrid.reef.standalone

import org.totalgrid.reef.client.ConnectionFactory
import org.totalgrid.reef.client.javaimpl.ConnectionWrapper
import net.agileautomata.executor4s._

/**
 * a nasty single that
 */
object InMemoryNode {

  lazy val connection = systemOption.get.connection()
  lazy val javaConnectionFactory = new ConnectionFactory {
    lazy val javaConnection = new ConnectionWrapper(connection)
    def connect() = javaConnection

    def terminate() {}
  }
  lazy val userSettings = systemOption.get.userSettings

  private var systemOption = Option.empty[IntegratedSystem]
  private var delayedShutdown = Option.empty[Timer]
  private var exeOption = Option.empty[ExecutorService]

  def initialize(configFile: String, resetFirst: Boolean, fileName: String) = {
    delayedShutdown.foreach { _.cancel }
    if (systemOption.isEmpty) {
      val exe = Executors.newResizingThreadPool(1.minutes)
      val system = new IntegratedSystem(exe, configFile, 1, 1, resetFirst)
      exeOption = Some(exe)
      systemOption = Some(system)

      system.start()

      system.loadModel(fileName)

      true
    } else {
      false
    }
  }

  def startShutdown() {
    delayedShutdown.foreach { _.cancel }
    exeOption.foreach { exe =>
      delayedShutdown = Some(exe.schedule(10000.milliseconds) {
        try {
          systemOption.foreach { _.stop() }
        } finally {
          exeOption.foreach { _.shutdown() }
        }
      })
    }
  }
}