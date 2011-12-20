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
 * RPC Interfaces to the most commonly used Reef Services.
 * 
 * The reef system is fundamentally constructed using a REST approach. This means the system is modeled using nouns and
 * a common set of 4 verbs GET, PUT, POST, DELETE. This is a very powerful approach and allows us to create simple and
 * robust services and have a relatively simple protocol to make interoperability across a wide range of languages
 * possible. While this model is powerful it can also be confusing to an application developer to determine exactly how
 * to map from a concrete goal ("Get list of Substations") to the noun+verb combination necessary to implement that
 * request (GET Entity type=Substation). In order to reduce the cognitive load on application developers we have
 * provided a suite of interfaces that include requests for most of the common use cases. These interfaces are not
 * exhaustive, and do not attempt to expose all of the functionality available to clients of a service, instead they
 * serve as a starting point and suite of examples to pull from to make more complex requests when necessary.
 * 
 * <p>
 * Some applications have to write custom requests to support some use cases. Many very "chatty" sets of requests can be implemented with a single
 * more complex request. We envision application developers extending the interfaces with their own custom functions for their application.
 * 
 * If that is necessary the application developer can use the ClientOperations interface to make and handle the results of those more complex queries.
 */
package org.totalgrid.reef.client.service;

