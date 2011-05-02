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
package org.totalgrid.reef.util

trait ConfigReader {

  def getProp(key: String): Option[String]

  // TODO: remove defaults from most configuration options
  def getString(key: String, default: String) = getProp(key) getOrElse (default)
  def getInt(key: String, default: Int) = getProp(key).map(s => s.toInt) getOrElse (default)
  def getLong(key: String, default: Long) = getProp(key).map(s => s.toLong) getOrElse (default)
  def getBoolean(key: String, default: Boolean) = getProp(key).map(s => s.toBoolean) getOrElse (default)
}

