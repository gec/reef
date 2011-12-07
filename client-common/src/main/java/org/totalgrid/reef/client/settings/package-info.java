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
/**
 * <p>
 * Provides the settings classes used to connect and authenticate with a reef server.
 * </p>
 * 
 * <p>
 * Most simple applications will need to use the AmqpSettings and UserSettings classes, only more advanced applications that are using the
 * heartbeating system need to user the NodeSettings class.
 * </p>
 * 
 * <p>
 * In most installations agents of a similar class (applications, HMIs, service providers, human operators) will share the same AmqpSettings. The
 * AmqpSettings authentication provides a very high level of authorization to the system, the user level authorization provides the low level,
 * individual resource level of authentication.
 * </p>
 * 
 * <p>
 * A helpful analogy is the keys for an apartment building, all residents will have the same key to the lobby door and gym but they each have a custom
 * key for their apartment. The AmqpSettings is like the lobby door key, lets you into the building, but then you still need an individual apartment
 * key, the UserSettings object, to actually do anything in reef. To extend the analogy even further, we can think of all of the residents as our
 * human operators and applications, they are only allowed into the common/public areas and allowed to use the services like elevators and trash
 * chutes. The janitors and owners of the building will have a different set of keys that gives them access to the basements and maintenance ducts.
 * Service providers in reef are like the janitors and they have the AmqpSettings that allow them greater access to the internals of the reef system.
 * </p>
 */
package org.totalgrid.reef.client.settings;