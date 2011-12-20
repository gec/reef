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
 * The Async versions of all of the interfaces are exactly the same as the regular interfaces except
 * the return values are instantly returned Promises that can then be awaited on. This allows an
 * application to be written that doesn't need to block a thread for each request to reef.
 * 
 * The request to the server has been started as soon as the api call is made so the result may be
 * ready at any time, including as soon as the api call has returned (if the server responds very quickly).
 * 
 * An application can mix-and-match which service APIs it is using depending on the use cases. It is
 * generally recommended to use the standard synchronous APIs unless taking advantage of the
 * Promise.listen() calls, otherwise it is possible to miss a .await call
 */
package org.totalgrid.reef.client.service.async;

