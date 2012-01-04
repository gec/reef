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
package org.totalgrid.reef.executor

import org.osgi.framework.{ BundleActivator, BundleContext }
import com.weiglewilczek.scalamodules._
import net.agileautomata.executor4s._

final class Activator extends BundleActivator {

  val exe = Executors.newResizingThreadPool(1.minutes)

  def start(context: BundleContext) = context.createService(exe, interface1 = interface[Executor])

  def stop(context: BundleContext) = exe.terminate()
}