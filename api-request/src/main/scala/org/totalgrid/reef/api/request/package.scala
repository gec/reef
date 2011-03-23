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
package org.totalgrid.reef.api

/**
 * The REEF system is fundamentally constructed using a REST approach. This means the system is modeled using nouns and
 * a common set of 4 verbs GET, PUT, POST, DELETE. This is a very powerful approach and allows us to create simple and
 * robust services and have a relatively simple protocol to make interoperability across a wide range of languages
 * possible. While this model is powerful it can also be confusing to an application developer to determine exactly how
 * to map from a concrete goal ("Get list of Substations") to the noun+verb combination necessary to implement that
 * request (GET Entity{type: "Substation"}).
 *
 * In order to reduce the cognitive load on application developers we have provided a suite of interfaces that include
 * requests for most of the common use cases. These interfaces are not exhaustive, and do not attempt to expose all of
 * the functionality available to clients of a service. It is recommended  to use these interfaces as a starting point
 * for working with the REEF system but we fully expect some applications to have to write custom requests to support
 * some use cases. Many very "chatty" sets of requests can be implemented with a single more complex request. We
 * envision application developers extending the interfaces with their own custom functions for their application.
 *
 * To add those custom functions it is best to understand the requests we have "baked in". This can be done in a few ways:
 * <ul>
 * <li> read the code in XxxxxRequestBuilders and XxxxxServiceImpl classes </li>
 * <li> read the "generated proto request/reply" documents, they show live versions of the proto messages that are sent over the wires </li>
 * <li> read the documentation on the protobuf files (*.proto) </li>
 * </ul>
 * Once a new query has been figured out, the XxxxxService class can be extended to add in the new requests.
 */
package object request