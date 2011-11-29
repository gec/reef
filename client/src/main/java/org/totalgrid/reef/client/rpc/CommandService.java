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
package org.totalgrid.reef.client.rpc;

import java.util.List;

import org.totalgrid.reef.clientapi.exceptions.ReefServiceException;
import org.totalgrid.reef.proto.Commands.CommandAccess;
import org.totalgrid.reef.proto.Commands.CommandStatus;
import org.totalgrid.reef.proto.Commands.UserCommandRequest;
import org.totalgrid.reef.proto.Model.Command;
import org.totalgrid.reef.proto.Model.ReefUUID;
import org.totalgrid.reef.proto.Model.ReefID;
import net.agileautomata.executor4s.Cancelable;
import org.totalgrid.reef.client.rpc.commands.CommandRequestHandler;

/**
 *
 * <p>
 *   Service for issuing commands and controls on field devices.</p>
 *
 * <h3>Overview</h3>
 *
 * <p>SCADA systems use commands to affect changes in the field devices. Commands are usually executed in the field
 * by the same equipment that is generating measurements. Each command usually represents one action that can
 * be taken in the field like tripping a breaker or raising the setpoint voltage.
 *
 * <h3>Command</h3>
 * <p>
 *   The term "command" is a specific command on a specific field device instance. A command "name" is the
 *   specific command for a device appended to the device name (ex: "substation1.breaker2.trip").</p>
 *
 * <h3>Select/Lock a Command</h3>
 * <p>
 *   An agent cannot execute a command until they first "select" the command to acquire an exclusive
 *   lock on the command. The terms "select" and "lock" can be used interchangeably. The exclusive lock
 *   is tied to the agent who acquired them and do not need to be passed with the command execute. The
 *   agent may execute the command from different login session (ex: two browser windows).</p>
 *
 * <ul>
 *   <li>A select is designed to be held for seconds up to minutes after which time it is deselected automatically.</li>
 *   <li>The agent whom created the select should release the select.</li>
 *   <li>A select is not automatically released after a command is executed.</li>
 *   <li>Command Denial locks do not timeout (see below).</li>
 * </ul>
 *
 * <h4>Usage</h4>
 * <p>Issue a command: select, execute, deselect.</p>
 * <pre>
 *    Command cmd = getCommandByName( "substation1.breaker2.trip");
 *    CommandAccess lock = createCommandExecutionLock( cmd);
 *    executeCommandAsControl( cmd);
 *    deleteCommandLock( lock);
 * </pre>
 *
 * <p>Get a list of commands for a device.</p>
 * <pre>
 *    ???
 * </pre>
 *
 * <h3>Command Denial Lock</h3>
 * <p>
 *   when an operator needs to make sure no one will execute any of a set of commands they
 *   create a system-wide "denial lock" on those commands. This will prevent all operators and
 *   applications from issuing a command or selecting those commands. To
 *   execute those commands the lock will need to be deleted.</p>
 *
 * <p>
 *   By default, Denial locks do not timeout like "execution locks".</p>
 *
 *
 *  Tag for api-enhancer, do not delete: !api-definition!
 */
public interface CommandService
{

    /**
     * Select (i.e. lock) a list of commands. The system default lock expiration time is used with this method.
     *
     * When an operator needs to issue a command they must first establish exclusive access to
     * the command (or set of commands). Getting a system-wide "execution lock" gives the operator
     * exclusive access to execute the locked commands to make sure no other operator/agent is operating
     * on those commands at the same time. A well behaved client should delete the lock once the user
     * is satisfied with the command execution. The locks are designed to be held for seconds up to
     * minutes, possibly directly related to the life-cycle of execution dialogs. Execution locks expire
     * after some period of time (system configurable but usually 30 seconds). It is the clients job
     * to lock the correct set of commands.
     *
     * @param cmds  List of commands. A "command" is a specific command on a specific device instance.
     * @return an object describing the lock.
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess createCommandExecutionLock( List<Command> cmds ) throws ReefServiceException;

    /**
     * Select (i.e. lock) a list of commands and lock for the supplied expiration time.
     *
     * When an operator needs to issue a command they must first establish exclusive access to
     * the command (or set of commands). Getting a system-wide "execution lock" gives the operator
     * exclusive access to execute the locked commands to make sure no other operator/agent is operating
     * on those commands at the same time. A well behaved client should delete the lock once the user
     * is satisfied with the command execution. The locks are designed to be held for seconds up to
     * minutes, possibly directly related to the life-cycle of execution dialogs. Execution locks expire
     * after some period of time (system configurable but usually 30 seconds). It is the clients job
     * to lock the correct set of commands.
     *
     * @param cmds  List of commands. A "command" is a specific command on a specific device instance.
     * @param expirationTimeMilli milliseconds to lock the supplied commands
     * @return an object describing the lock.
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess createCommandExecutionLock( List<Command> cmds, long expirationTimeMilli ) throws ReefServiceException;

    /**
     * Select (i.e lock) a command.
     *
     * @param command  A "command" is a specific command on a specific device instance.
     * @return an object describing the lock.
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess createCommandExecutionLock( Command command ) throws ReefServiceException;

    /**
     * Select (i.e lock) a command.
     *
     * @param command  A "command" is a specific command on a specific device instance.
     * @param expirationTimeMilli milliseconds to lock the supplied commands
     * @return an object describing the lock.
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess createCommandExecutionLock( Command command, long expirationTimeMilli ) throws ReefServiceException;

    /**
     * Deselect a command or set of commands. When we have completed the execution of a command
     * we delete the system-wide lock we had. This releases the resource so other agents can
     * access those commands.
     *
     * @param ca the lock to be deleted
     * @return the deleted lock
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess deleteCommandLock( CommandAccess ca ) throws ReefServiceException;

    /**
     * Deselect a command or set of commands. When we have completed the execution of a command
     * we delete the system-wide lock we had. This releases the resource so other agents can
     * access those commands.
     * @param commandUid
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess deleteCommandLock( ReefID commandUid ) throws ReefServiceException;

    /**
     * Clear all of the command locks in the system. This is a dangerous operation that should only
     * be preformed in test environments. In production systems this will fail if any other uses
     * have locks (since we don't have permission to delete other peoples locks).
     *
     * @return the deleted locks
     * @throws ReefServiceException if an error occurs
     */
    List<CommandAccess> clearCommandLocks() throws ReefServiceException;

    // TODO add checks for control vs. setpoint execution - backlog-62
    /**
     * Execute a control on a field device.
     * One type of Command in SCADA systems are referred to as "Controls". These can be thought of as
     * buttons on pieces of field equipment. Examples are sending a "TRIP" control to a breaker, a
     * "RESET" control to a router or a "TAP_UP" control to a transformer tap changer. They carry no
     * data other than their ID. If the user tries to issue a "control" request on a non-control
     * command it will cause an exception.
     *
     * @param cmd  Name of the command
     * @return the status of the execution, SUCCESS is only non-failure (throw exception?)
     * @throws ReefServiceException if an error occurs
     */
    CommandStatus executeCommandAsControl( Command cmd ) throws ReefServiceException;

    /**
     * Execute a setpoint on a field device.
     * One type of Command in SCADA systems are referred to as "Setpoints". These can be thought of
     * as dials on a piece of field equipment. Examples are setting a thermostat to a new temperature
     * or changing the voltage on a transformer. They carry an extra piece of data other than their name
     * telling the field device what value we want to set the "dial" to. If the setpoint is actually
     * expecting an integer value the passed in double value will be floored.
     *
     * @param cmd    The name of the command
     * @param value  Value of the setpoint
     * @return the status of the execution, SUCCESS is only non-failure (throw execption?)
     * @throws ReefServiceException if an error occurs
     */
    CommandStatus executeCommandAsSetpoint( Command cmd, double value ) throws ReefServiceException;

    /**
     * Execute a setpoint on a field device.
     * One type of Command in SCADA systems are referred to as "Setpoints". These can be thought of
     * as dials on a piece of field equipment. Examples are setting a thermostat to a new temperature
     * or changing the voltage on a transformer. They carry an extra piece of data other than their name
     * telling the field device what value we want to set the "dial" to.
     *
     * @param cmd    the name of the command
     * @param value Value of the setpoint
     * @return the status of the execution, SUCCESS is only non-failure (throw execption?)
     * @throws ReefServiceException if an error occurs
     */
    CommandStatus executeCommandAsSetpoint( Command cmd, int value ) throws ReefServiceException;

    /**
     * Select a field device so it will not accept any commands.
     * When an operator needs to make sure no one will execute any of a set of commands they
     * create a system-wide "denial lock" on those commands. This will prevent all operators and
     * applications from issuing a command or getting an execution lock on those commands. To
     * execute those commands the lock will need to be deleted.
     *
     * <p>By default Denial locks do not timeout like "execution locks".</p>
     *
     * @param cmds list of command names
     * @return an object describing the lock
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess createCommandDenialLock( List<Command> cmds ) throws ReefServiceException;

    /**
     * Get a list of all command locks in system
     * @throws ReefServiceException if an error occurs
     */
    List<CommandAccess> getCommandLocks() throws ReefServiceException;

    /**
     * Get a command lock by UUID
     * @param id the id of the command to lock
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess getCommandLockById( ReefID id ) throws ReefServiceException;

    /**
     * Get the command lock (if it exists) for a Command
     *
     * @param cmd the command to find the lock for
     * @return the command lock or null if no matching lock found
     * @throws ReefServiceException if an error occurs
     */
    CommandAccess findCommandLockOnCommand( Command cmd ) throws ReefServiceException;

    /**
     * Gets a list of all command locks that are active for any of the commands. This is useful
     * to determine who is holding locks on the command you are trying to use.
     * @param cmds the commands to find the lock for
     * @throws ReefServiceException if an error occurs
     */
    List<CommandAccess> getCommandLocksOnCommands( List<Command> cmds ) throws ReefServiceException;

    /**
     * Get a recent history of issued commands. Information returned is who issued them and what
     * the final status was.
     * @throws ReefServiceException if an error occurs
     */
    List<UserCommandRequest> getCommandHistory() throws ReefServiceException;

    /**
     * Get a recent history for a particular command. Information returned is who issued them and what
     * the final status was.
     * @throws ReefServiceException if an error occurs
     */
    List<UserCommandRequest> getCommandHistory( Command cmd ) throws ReefServiceException;

    /**
     * Get a list of available commands in the system
     * @throws ReefServiceException if an error occurs
     */
    List<Command> getCommands() throws ReefServiceException;

    /**
     * Get a command object by name.
     *
     * @param name  Command name (example: "substation1.breaker2.trip").
     * @return command associated with the supplied name
     * @throws ReefServiceException if an error occurs
     */
    Command getCommandByName( String name ) throws ReefServiceException;

    /**
     * retrieve all commands that are have the relationship "owns" to the parent entity
     *
     * @param parentUUID uuid of parent entity
     * @return commands owned by parentEntity
     */
    List<Command> getCommandsOwnedByEntity( ReefUUID parentUUID ) throws ReefServiceException;

    /**
     * retrieve all commands that have the relationship "source" to the endpoint
     *
     * @param endpointUuid uuid of endpoint
     * @return all commands that are related to endpoint
     */
    List<Command> getCommandsBelongingToEndpoint( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * retrieve all commands that have the relationship "feedback" to the point
     *
     * @param pointUuid uuid of endpoint
     * @return all commands that are related to point
     */
    List<Command> getCommandsThatFeedbackToPoint( ReefUUID pointUuid ) throws ReefServiceException;

    /**
     * Binds a commandHandler to the broker that will be responsible for all commands on a single endpoint. The
     * same command handler can be used for endpoints if desired. All commands received on this channel are
     * already authorized by the services and should executed as soon as possible.
     *
     * @param endpointUuid uuid of the endpoint to handle all commands
     * @param handler an application controled object that
     * @return a cancelable that should be canceled when the application is done being a command handler for
     *         the endpoint
     */
    Cancelable bindCommandHandler( ReefUUID endpointUuid, CommandRequestHandler handler ) throws ReefServiceException;

}