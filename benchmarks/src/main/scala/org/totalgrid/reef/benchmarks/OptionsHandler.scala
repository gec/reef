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
package org.totalgrid.reef.benchmarks

import java.util.Properties
import org.totalgrid.reef.client.settings.util.PropertyLoading

/**
 * options loading trait that makes loading lots of parameters from a file easy to read.
 * All names will be prepended with a baseName + "." + name.
 * May be useful to other applications but would need to handle optional values better.
 */
trait OptionsHandler {
  def getBool(name: String): Boolean
  def getInt(name: String): Int
  def getString(name: String): String
  def getIntList(name: String): List[Int]
  def getStringList(name: String): List[String]

  /**
   * return a new options handler that has name attached to its basename
   */
  def subOptions(name: String): OptionsHandler
}

/**
 * trivial implementation of OptionsHandler
 */
class SimpleOptionsHandler(props: Properties, baseName: String) extends OptionsHandler {

  private def fullName(name: String) = baseName + "." + name

  def getBool(name: String) = PropertyLoading.getBoolean(fullName(name), props)

  def getInt(name: String) = PropertyLoading.getInt(fullName(name), props)

  def getString(name: String) = PropertyLoading.getString(fullName(name), props)

  def getIntList(name: String) = PropertyLoading.getString(fullName(name), props).split(",").toList.map { Integer.parseInt(_) }

  def getStringList(name: String) = PropertyLoading.getString(fullName(name), props).split(",").toList

  def subOptions(name: String) = new SimpleOptionsHandler(props, fullName(name))
}