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

import net.agileautomata.executor4s.Executor
import scala.collection.immutable.Queue

/**
 * implements ProcessManager using TaskStates
 */
class SimpleProcessManager(exe: Executor) extends ProcessManager {

  private var tasks = Map.empty[Process, TaskState]

  private var parentTasks = Queue.empty[TaskState]

  private var started = false

  def addProcess(task: Process) = this.synchronized {
    val ts = new TaskState(task, this, exe)
    tasks += task -> ts
    parentTasks = parentTasks.enqueue(ts)
    if (started) ts.start()
  }

  def addChildProcess(parent: Process, childTask: Process) = this.synchronized {
    val parentTaskState = lookupTask(parent)

    val childTaskState = tasks.get(childTask) match {
      case Some(taskState) => taskState
      case None =>
        val ts = new TaskState(childTask, this, exe)
        tasks += childTask -> ts
        ts
    }

    parentTaskState.addChild(childTaskState)

  }

  def removeProcess(task: Process) = this.synchronized {
    tasks.get(task).foreach { removeTaskState(_) }
  }

  private def removeTaskState(taskState: TaskState) {
    taskState.children.foreach { removeTaskState(_) }

    taskState.parent.foreach { _.removeChild(taskState) }

    if (taskState.isActive) {
      taskState.stop()
    }

    tasks -= taskState.payloadLogic
    parentTasks = parentTasks.filterNot(_ == taskState)
  }

  private def lookupTask(task: Process) = {
    tasks.get(task).getOrElse(throw new IllegalArgumentException("unknown task " + task.name))
  }

  def failProcess(task: Process) = this.synchronized {
    val taskState = lookupTask(task)

    taskState.fail()
  }

  def start() = this.synchronized {
    started = true
    val toStart = parentTasks.filter { t => t.shouldBeStarted }

    toStart.foreach { _.start() }
  }

  def stop() = {
    this.synchronized {
      started = false
    }
    parentTasks.foreach { _.stop() }
  }

  def isRunning = this.synchronized { started }
}