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
package org.totalgrid.reef.client.sapi.client

trait HasHeaders {
  def getHeaders: BasicRequestHeaders
  def setHeaders(headers: BasicRequestHeaders): Unit
  def modifyHeaders(modify: BasicRequestHeaders => BasicRequestHeaders): Unit
}

trait DefaultHeaders extends HasHeaders {

  /** The default request headers */
  private var defaultHeaders = BasicRequestHeaders.empty

  /** The current value of the headers */
  def getHeaders = defaultHeaders

  /** Set the default request headers */
  def setHeaders(headers: BasicRequestHeaders) = defaultHeaders = headers

  /** Provide a little syntactic sugar for change the headers */
  def modifyHeaders(modify: BasicRequestHeaders => BasicRequestHeaders) = setHeaders(modify(defaultHeaders))

}
