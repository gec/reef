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
package org.totalgrid.reef.app.process

/**
 * a process manager is used to manage a group of processes.
 */
trait ProcessManager extends ErrorHandlerManager {

  /**
   * add a "top level" process, it will be started/stopped depending on if manager is running
   */
  def addProcess(process: Process)

  /**
   * add a child of another process, it will only be run if parent is running
   */
  def addChildProcess(parent: Process, childProcess: Process)

  /**
   * removes a task "top-level" or child. If a parent task is removed, all of its children are removed.
   * Before a task is removed cleanup() on it and all of its children
   */
  def removeProcess(process: Process)

  /**
   * when a process wants to fail itself (or an external agent wants to force a failure) they will use
   * the manager to ask for the failure to be processed.
   */
  def failProcess(process: Process)

  /**
   * starts the process manager, all "top level" processes will be started and restarted while we are still
   * running. call returns immediatley.
   */
  def start()

  /**
   * stops all of the running processes and blocks until they are all stopped. A manager can be stopped and
   * started.
   */
  def stop()

  /**
   * whether the manager is currently running or not
   */
  def isRunning: Boolean

  /**
   * when an error is encountered we will pass it through to any registered error handlers, so they can see
   * why their process (tree) is failing.
   */
  def reportError(process: Process, msg: String, exception: Option[Exception]) = {
    handleError(process.name + " - " + msg, exception)
  }

}