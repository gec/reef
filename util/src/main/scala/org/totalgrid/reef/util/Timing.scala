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

import org.totalgrid.reef.util.Timing.Stopwatch

/**
 * methods for determining elapsed time for a supplied function.
 */
object Timing {

  /**
   * very simple class to make measuring elapsed time easier in benchmark and test code
   */
  class Stopwatch {
    private var start = System.nanoTime()

    /**
     * reset the start time for the elapsed counters
     */
    def reset() = start = System.nanoTime()

    /**
     * returns time in milliseconds
     */
    def elapsed = convertNanoToMilli(System.nanoTime() - start)
  }

  /**
   * Runs a block of code and returns how long it took in milliseconds (not the return value of the block)
   */
  def benchmark[A](fun: => A): Long = {
    val stopwatch = new Stopwatch
    fun
    stopwatch.elapsed
  }

  /**
   * Runs a block of code and prints (to stdout) how long the operation took
   */
  def time[A](name: String)(fun: => A): A =
    time(x => println(name + " took " + x + "ms"))(fun)

  /**
   * Runs a block of code and passes the length of time it took to another function
   */
  def time[A](timingFun: Long => Unit)(fun: => A): A = {
    val stopwatch = new Stopwatch
    val ret = fun
    timingFun(stopwatch.elapsed)
    ret
  }

  def convertNanoToMilli[A](value: Long): Long = scala.math.floor(value / 1000000d).toLong

}