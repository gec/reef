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
package org.totalgrid.reef.util

/**
 * TODO: remove all references to buildenv
 */
object BuildEnv {

  /**
   * common base class for connection info classes so they can be passed around generically until we need to load them
   */
  class ConnInfo

  /**
   * default "environment" development/test/production, overriden by RAILS_ENV
   */
  def environment() = {
    Option(System.getenv("RAILS_ENV")) getOrElse "development"
  }

  /**
   * looks for a system property that has the same name as the file or returns the default
   */
  def path(name: String) = {
    Option(System.getenv(name)) getOrElse configPath + name
  }

  def cfgName(base: String, subPid: String, env: String): String = {
    env match {
      case "test" => base + ".test.cfg"
      case "development" => base + "." + subPid + ".cfg"
    }
  }

  /**
   * common config file reader,
   */
  def cfgFileReader(subPid: String, env: String) = {
    new FileConfigReader(path(cfgName("org.totalgrid.reef", subPid, env)))
  }

  /**
   * location of directory containing the yml configuration files
   */
  def configPath = {
    if (System.getProperty("config.path") != null)
      System.getProperty("config.path")
    else
      "../"
  }
}