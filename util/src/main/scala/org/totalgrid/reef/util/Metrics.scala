/**
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

class UnHookedException(msg: String) extends Exception(msg)

object MetricsHooks {
  /// 
  var forceHooks = false

  sealed abstract class HookType
  case object Counter extends HookType
  case object Value extends HookType
  case object Average extends HookType
}

trait MetricsHookSource {

  def getSinkFunction(hookName: String, typ: MetricsHooks.HookType): (Int) => Unit
}

class SilentHookSource extends MetricsHookSource {
  def getSinkFunction(hookName: String, typ: MetricsHooks.HookType): (Int) => Unit = { (i: Int) =>
    {}
  }
}

/** Base trait shared by MetricsHooks and HookedObjectContainer so classes concerned with setting the 
 * sources can treat hooked objects and hookedcontainers the same
 */
trait HookableObject {

  /**
   * Sets the source for metrics sink objects
   */
  def setHookSource(source: MetricsHookSource)
}

/** 
 * this trait simplifies the common task of having one or more child objects that generate metrics values
 * and allows the actual hooking to be delayed until after construction without requiring a complex component
 * to expose its "hookable" sub objects. Of course all objects in HookedObjectContainer should expect to
 * receive the same hook source (which usually means the same base name: basename.hook_name)
 */
trait HookedObjectContainer extends HookableObject {
  /**
   * all hooked objects will have their setHookSource function called when the parents setHookSource is called
   */
  def addHookedObject(obj: HookableObject)

  /**
   * all hooked objects will have their setHookSource function called when the parents setHookSource is called
   */
  def addHookedObject(objs: List[HookableObject])
}

/**
 * Both contains other HookedObjects and provides hook functions for internal use.
 */
trait MetricsHookContainer extends MetricsHooks with HookedObjectContainer {
  private var hookedObjects = List.empty[HookableObject]

  def addHookedObject(obj: HookableObject) {
    hookedObjects = obj :: hookedObjects
  }

  def addHookedObject(objs: List[HookableObject]) {
    hookedObjects = objs ::: hookedObjects
  }

  override def setHookSource(source: MetricsHookSource) = {
    super.setHookSource(source: MetricsHookSource)
    hookedObjects.foreach(_.setHookSource(source))
  }
}

object MetricsHookFunctions {
  /** blank function used when no hook is needed for the function
   * TODO: change getSinkFunction signature to return Optional function
   */
  val nop = (i: Int) => {}
}

/**
 * provides the interface used by client code to setup hooks for values, counters and timing
 * operations.
 */
trait MetricsHookFunctions {

  protected def getHook(name: String, typ: MetricsHooks.HookType): (Int) => Unit

  protected def valueHook(name: String): (Int) => Unit = {
    getHook(name, MetricsHooks.Value)
  }

  protected def counterHook(name: String): (Int) => Unit = {
    getHook(name, MetricsHooks.Counter)
  }

  protected def averageHook(name: String): (Int) => Unit = {
    getHook(name, MetricsHooks.Average)
  }

  protected def timingHook[T](name: String): (=> T) => T = {
    val metric = averageHook(name)
    if (metric != MetricsHookFunctions.nop) Timing.time(t => metric(t.toInt)) _
    else {
      /// only necessary to make scala compiler happy, couldn't define anonymously
      def nothing[T]()(block: => T) = { block }
      nothing() _
    }
  }
}

/**
 * Implementation of the MetricsHookFunctions interface where the functions are defined
 * lazily allowing a hook source to be attached after construction.
 * TODO: rename to LazyMetricsHooks
 */
trait MetricsHooks extends HookableObject with MetricsHookFunctions {

  private var hookSource: Option[MetricsHookSource] = None

  protected def getHook(name: String, typ: MetricsHooks.HookType): (Int) => Unit = {
    hookSource match {
      case Some(source) => source.getSinkFunction(name, typ)
      case None =>
        if (MetricsHooks.forceHooks) {
          throw new UnHookedException("Unbound metric hook : " + name)
        } else {
          // return a no-op function
          MetricsHookFunctions.nop
        }
    }
  }

  def setHookSource(source: MetricsHookSource) = {
    hookSource = Some(source)
  }

}

/**
 * MetricsHookFunctions base class that allows the hook functions to be directly attached
 * to a hook source so no lazy indirection is necessary
 */
class StaticMetricsHooksBase(source: MetricsHookSource) extends StaticMetricsHooks with MetricsHookFunctions {
  val metricsSource = source
}

trait StaticMetricsHooks extends MetricsHookFunctions {
  val metricsSource: MetricsHookSource
  def getHook(name: String, typ: MetricsHooks.HookType): (Int) => Unit = {
    metricsSource.getSinkFunction(name, typ)
  }
}