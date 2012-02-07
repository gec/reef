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
 * A unit or work to be used by a process manager. Process "trees" may be constructed ahead of time
 * or built up and torn down as the processes run. This is the most basic type of Process, usually
 * consumer applications should use the more specific process classes like RetryableProcess and
 * OneShotProcess.
 */
trait Process {

  /**
   * name of the process to be included in log and error messages
   */
  def name: String

  /**
   * called by the process manager to start the process. It is expected that processes will throw an
   * exception on failure while setting up. The process should hold onto the ProcessManager if may need
   * to fail itself later. It may also add children to itself and they will be started after this
   * method completes.
   */
  def setup(p: ProcessManager)

  /**
   * this method is expected to make a "best-effort" attempt to cleanup after the process. The cleanup
   * call will be made regardless of the reason for failure. This means that the resources needed to cleanup
   * (clients, files, etc) may have been lost so the cleanup process needs to be tolerant of those sorts of
   * errors. Exceptions thrown during cleanup are logged and discarded.
   */
  def cleanup(p: ProcessManager)

  /**
   * if the child has a failure we will fail this process, this allows building process trees where
   * we fail all of the parents if the lowest leaf dies. If false this allows the child process
   * to start/fail/stop without affecting parent.
   */
  def failIfChildFails = true

  /**
   * in some cases the setup() call failing is considered fatal and there is no point retrying
   * so we will fail this process to indicate to other process we have given up
   */
  def setupExceptionIsFailure = true

  /**
   * if we are configured to retry setup after a failure we will use this delay to determine how long
   * to wait. Specific classes should override this value.
   */
  def setupRetryDelay: Long = 1000

  /**
   * should we consider a failure to be terminal and not retry
   */
  def retryAfterFailure = true
}

/**
 * This is for processes that should be retried after any sort of failure.
 */
abstract class RetryableProcess(processName: String) extends Process {

  override def name = processName

  override def retryAfterFailure = true

  override def failIfChildFails = true
}

/**
 * this is for processes when we want to make one attempt at setting up and if it fails
 * we give up (usually because a parent taks will need to be restarted).
 */
abstract class OneShotProcess(processName: String) extends Process {

  override def name = processName

  override def setupExceptionIsFailure = true

  override def retryAfterFailure = false
}