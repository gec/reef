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

import org.totalgrid.reef.proto.Commands.{ CommandStatus, UserCommandRequest, CommandAccess }
import org.totalgrid.reef.proto.Model.Command
import org.totalgrid.reef.api.ReefServiceException

/**
 * To affect changes in the field devices SCADA systems use commands. Commands are usually executed in the field
 * by the same equipment that are generating measurements. Each command usually represents one action that can
 * be taken in the field like tripping a breaker or raising the setpoint voltage. To execute a control the agent
 * must first acquire an exclusive lock on the command. Acquiring these locks is usually known as "Selecting a
 * Command", the two terms are used interchangably. These locks can be at any granularity: command, related-
 * commands, equipment, or equipment group.
 *
 * These locks are tied to the user who acquired them and do not need to be passed with the command request.
 * One thing to note is that if an operator had 2 open windows, locked the command in one window, he would
 * be allowed to execute the command in the other.
 *
 * TODO: Change command names to UUIDs
 */
trait CommandService {

  /**
   * When an operator needs to issue a command they must first establish exclusive access to
   * the command (or set of commands). Getting a system-wide "execution lock" gives the operator
   * exclusive access to execute the locked commands to make sure no other operator/agent is operating
   * on those commands at the same time. A well behaved client should delete the lock once the user
   * is satisfied with the command execution. The locks are designed to be held for seconds upto
   * minutes, possibly directly related to the lifecycle of execution dialogs. Execution locks expire
   * after some period of time (system configurable but usually 30 seconds). It is the clients job
   * to lock the correct set of commands.
   * @param ids list of command names
   * @return an object describing the lock
   */
  @throws(classOf[ReefServiceException])
  def createCommandExecutionLock(cmds: java.util.List[Command]): CommandAccess

  /**
   * same as createCommandExecutionLock
   */
  @throws(classOf[ReefServiceException])
  def createCommandExecutionLock(cmd: Command): CommandAccess

  /**
   *  when we have completed the execution of a command we delete the system-wide lock we had; this
   * releases the resource so other agents can access those commands.
   * @param ca the lock to be deleted
   * @return the deleted lock
   */
  @throws(classOf[ReefServiceException])
  def deleteCommandLock(ca: CommandAccess): CommandAccess

  /**
   * same as deleteCommandLock
   */
  @throws(classOf[ReefServiceException])
  def deleteCommandLock(uuid: ReefUUID): CommandAccess

  /**
   * Clear all of the command locks in the system. This is a dangerous operation that should only be preformed in test
   * environments. In production systems this will fail if any other uses have locks (since we dont have permission to
   * delete other peoples locks).
   * @return the deleted locks
   */
  @throws(classOf[ReefServiceException])
  def clearCommandLocks(): java.util.List[CommandAccess]

  /**
   *  One type of Command in SCADA systems are refered to as "Controls". These can be thought of as
   * buttons on pieces of field equipment. Examples are sending a "TRIP" control to a breaker, a
   * "RESET" control to a router or a "TAP_UP" control to a transformer tap changer. They carry no
   * data other than their ID. If the user tries to issue a "control" request on a non-control
   * command it will cause an execption.
   * TODO: add checks for control vs. setpoint execution
   *
   * @param id the name of the command
   * @return the status of the execution, SUCCESS is only non-failure (throw execption?)
   */
  @throws(classOf[ReefServiceException])
  def executeCommandAsControl(cmd: Command): CommandStatus

  /**
   * One type of Command in SCADA systems are refered to as "Setpoints". These can be thought of
   * as dials on a piece of field equipment. Examples are setting a thermostat to a new temperature
   * or changing the channel on a TV. They carry an extra piece of data other than their name
   * telling the field device what value we want to set the "dial" to. If the setpoint is actually
   * expecting an integer value the passed in double value will be floored.
   *
   * @param id the name of the command
   * @return the status of the execution, SUCCESS is only non-failure (throw execption?)
   */
  @throws(classOf[ReefServiceException])
  def executeCommandAsSetpoint(cmd: Command, value: Double): CommandStatus

  /**
   * when an operator needs to make sure no one will execute any of a set of commands they
   * create a system-wide "denial lock" on those commands. This will prevent all operators and
   * applications from issuing a command or getting an execution lock on those commands. To
   * execute those commands the lock will need to be deleted. By default Denial locks do not
   * timeout like "execution locks".
   *
   * @param ids list of command names
   * @return an object describing the lock
   */
  @throws(classOf[ReefServiceException])
  def createCommandDenialLock(cmds: java.util.List[Command]): CommandAccess

  /**
   * get a list of all command locks in system
   */
  @throws(classOf[ReefServiceException])
  def getCommandLocks(): java.util.List[CommandAccess]

  /**
   * get a command locks by UUID
   */
  @throws(classOf[ReefServiceException])
  def getCommandLock(uuid: ReefUUID): CommandAccess

  /**
   * get the command lock (if it exists) for a Command
   *
   * TODO: figure out how to handle null return values in scala
   *
   * @return either the command lock or null
   */
  @throws(classOf[ReefServiceException])
  def getCommandLockOnCommand(cmd: Command): CommandAccess

  /**
   * gets a list of all command locks that are active for any of the commands. This is useful
   * to determine who is holding locks on the command you are trying to use.
   */
  @throws(classOf[ReefServiceException])
  def getCommandLocksOnCommands(cmds: java.util.List[Command]): java.util.List[CommandAccess]

  /**
   * get a recent history of issued commands. Information returned is who issued them, what
   * the final status was and when they were issued.
   */
  @throws(classOf[ReefServiceException])
  def getCommandHistory(): java.util.List[UserCommandRequest]

  /**
   * get a list of available commands in the system
   */
  @throws(classOf[ReefServiceException])
  def getCommands(): java.util.List[Command]

  /**
   * @param name command with name
   * @return command with name
   */
  @throws(classOf[ReefServiceException])
  def getCommandByName(name: String): Command
}