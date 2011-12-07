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
package org.totalgrid.reef.client.service.command;

import org.totalgrid.reef.client.service.proto.Commands.CommandRequest;

/**
 * When a command request is received from the server the application it will be sent to the registered
 * CommandRequestHandler. These requests have already authorized with the main reef services so it is
 * usually unnecessary to do further authorization and the command should be executed as soon as possible.
 */
public interface CommandRequestHandler
{

    /**
     * when a command request is received from the broker it will be shunted here, the application should start
     * processing the command and when it has completed call the callback with the final result. This function
     * will only be called once at a time so if the commands take a long time to process the handling should be
     * moved to a different thread. If you block this callback you will not get new requests.
     *
     * @param cmdRequest request object that has the command name and parameters
     * @param resultCallback specific callback that goes with this request
     */
    void handleCommandRequest( CommandRequest cmdRequest, CommandResultCallback resultCallback );

}
