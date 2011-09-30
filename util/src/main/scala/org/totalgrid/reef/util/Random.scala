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

object Random {
  /**
   * generate a random integer in the specified range.  Range must have a step value of 1 or the call will fail
   */
  def generate(range: Range): Int =
    {
      assert(range.step == 1)
      val value: Int = generateZeroBased(range.toSeq.size)
      value + range.head
    }

  /**
   * generates a random integer starting at 0 up to count specified exclusive of count
   */
  def generateZeroBased(maxValue: Int): Int =
    {
      assert(maxValue >= 1)
      generate(maxValue) - 1
    }

  /**
   * generates a random integer starting at 1 up to count specified
   */
  def generate(count: Int): Int =
    {
      assert(count > 0)
      val random = (math.random * count) + 0.5
      val value: Int = math.rint(random).round.asInstanceOf[Int]
      value
    }

}