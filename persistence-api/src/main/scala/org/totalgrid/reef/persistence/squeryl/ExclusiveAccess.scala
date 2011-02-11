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
package org.totalgrid.reef.persistence.squeryl

import org.squeryl.{ Query, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

/** grants exclusive access to a squeryl object to a single thread or process. It is 
 * hinged on there being some property of the sql object that indicates whether the 
 * action we want to take needs to occur. We try to get exclusive access to the object,
 * check the precondition, run the user block, then check that we have updated the 
 * object to no longer match the precondition (or else other threads would think
 * they were the first to get access to the object) 
 */
object ExclusiveAccess {

  class ObjectMissingException extends Exception
  class AcquireConditionNotMetException extends Exception
  class AcquireConditionStillValidException extends Exception

  def exclusiveAccess[A <: KeyedEntity[Long]](
    table: Table[A],
    id: Long,
    updateFun: A => Any,
    acquireCondition: A => Boolean)(lockFun: A => A): A = {

    // Wrap/unwrap in list
    val list = exclusiveAccess(table, List(id), updateFun, acquireCondition) { list =>
      List(lockFun(list.head))
    }
    list.head
  }

  def exclusiveAccess[A <: KeyedEntity[Long]](
    table: Table[A],
    ids: List[Long],
    updateFun: A => Any,
    acquireCondition: A => Boolean)(lockFun: List[A] => List[A]): List[A] = {

    transaction {
      // Select for update
      val objList = table.where(c => c.id in ids).forUpdate.toList

      // Fail if we have nothing
      if (objList.size < 1) throw new ObjectMissingException

      // Precondition on all objects
      if (objList.exists(!acquireCondition(_))) throw new AcquireConditionNotMetException

      // Get results, do any work inside the lock
      val results = lockFun(objList)

      // Postcondition on all objects
      if (results.exists(acquireCondition(_))) throw new AcquireConditionStillValidException

      // Call update for each row
      results.foreach(updateFun(_))

      results
    }
  }

}