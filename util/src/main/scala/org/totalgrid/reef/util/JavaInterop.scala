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
 * helpers for dealing with safely interacting with java clients of scala code
 */
object JavaInterop {

  /**
   * checks the value, returning it if notNull and throwing IllegalArgumentException otherwise
   */
  @throws(classOf[IllegalArgumentException])
  def notNull[A <: AnyRef](ref: A): A = notNull(ref, "")

  /**
   * checks the value, returning it if notNull and throwing IllegalArgumentException otherwise
   */
  @throws(classOf[IllegalArgumentException])
  def notNull[A <: AnyRef](ref: A, name: String): A = {
    if (ref == null) throw new IllegalArgumentException("Argument " + name + " cannot be null") else ref
  }
}