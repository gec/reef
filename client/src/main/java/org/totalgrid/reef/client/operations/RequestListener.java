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
package org.totalgrid.reef.client.operations;

import org.totalgrid.reef.client.Promise;
import org.totalgrid.reef.client.proto.Envelope;

/**
 * A RequestListener is notified of every low-level request to the server just after the request has been
 * issued to the server. The response Promise will not be ready yet and all listeners should use the Promise.listen
 * callback instead of calling await directly because this will break batchMode.
 *
 * All calls to this class will be performed using the caller thread that made the inital request. If sharing the same
 * listener across multiple client it should be made thread safe.
 */
public interface RequestListener
{
    /**
     * Callback when the a request is made
     * @param verb      which of the four verbs the request was made with
     * @param request   body of the request
     * @param response  promise to the eventual response object.
     */
    <T> void onRequest( Envelope.Verb verb, T request, Promise<Response<T>> response );
}
