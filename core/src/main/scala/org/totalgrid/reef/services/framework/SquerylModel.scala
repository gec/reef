/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.framework

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.models.{ ModelWithId }
import org.squeryl.Table
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess._

/**
 * Supertype for Proto/Squeryl models
 */
trait SquerylServiceModel[ProtoType <: GeneratedMessage, SqlType <: ModelWithId]
    extends ServiceModel[ProtoType, SqlType]
    with BasicSquerylModel[SqlType] { self: ModelObserver[SqlType] =>
}

/**
 * Interface for Squeryl-typed CRUD operations
 */
trait SquerylModel[SqlType <: ModelWithId] extends ModelCrud[SqlType]

/**
 * Implementation of CRUD (with lifecycle hooks) for Squeryl backend. Concrete classes
 * must provide Squeryl table reference as well as an implementation of ModelObserver
 */
trait BasicSquerylModel[SqlType <: ModelWithId]
    extends SquerylModel[SqlType]
    with ModelHooks[SqlType] with Logging { self: ModelObserver[SqlType] =>

  /**
   * Squeryl table associated with this model, can be used for custom lookup behavior
   */
  val table: Table[SqlType]

  /**
   * Create a model entry
   * 
   * @param entry   Object to be created
   * @return        Result of store creation/insertion
   */
  def create(entry: SqlType): SqlType = {
    val sql = preCreate(entry)
    val inserted = table.insert(sql)
    onCreated(inserted)
    postCreate(inserted)
    inserted
  }

  /**
   * Update an existing model entry
   * 
   * @param entry       Object to replace existing entry
   * @param existing    Existing entry to be replaced
   * @return            Result stored in data base and whether it was modified
   */
  def update(entry: SqlType, existing: SqlType): (SqlType, Boolean) = {
    val sql = preUpdate(entry, existing)
    if (isModified(entry, existing)) {
      sql.id = existing.id
      table.update(sql)
      onUpdated(sql)
      postUpdate(sql, existing)
      (sql, true)
    } else {
      info { entry + " not modified." }
      (existing, false)
    }
  }

  /**
   * Delete an existing entry
   *
   * @param entry       Existing entry to be deleted
   * @return            Result of store delete
   */
  def delete(entry: SqlType): SqlType = {
    preDelete(entry)
    table.delete(entry.id)
    onDeleted(entry)
    postDelete(entry)
    entry
  }

  /**
   * Lock and update a set of entries.
   * 
   * @param existing            List of entries to be updated
   * @param acquireCondition    Condition that must be true for all entries at time of acquisition and false at release
   * @param fun                 Update logic to be performed during lock, transforms acquired entries to list of updated entries
   * @return                    Entries that result from update
   */
  def exclusiveUpdate(existing: List[SqlType], acquireCondition: SqlType => Boolean)(fun: List[SqlType] => List[SqlType]): List[SqlType] = {

    val ids = existing.map(_.id)

    // Select for update
    val objList = table.where(c => c.id in ids).forUpdate.toList
    val list = table.where(c => c.id in ids).toList // TODO: figure out scala cloning

    // Fail if we have nothing
    if (objList.size < 1) throw new ObjectMissingException

    // Precondition on all objects
    if (objList.exists(!acquireCondition(_))) throw new AcquireConditionNotMetException

    // Get results, do any work inside the lock
    val results = fun(objList)

    if (results.length != objList.length) throw new Exception("Updated entries must be 1 to 1 map from input list")

    // Postcondition on all objects
    if (results.exists(acquireCondition(_))) throw new AcquireConditionStillValidException

    // Do the update, get the list of actual rows (after model hooks)
    val retList = results.zip(list).map {
      case (entry, previous) =>
        // Assert this is the same table row
        if (entry.id != previous.id)
          throw new Exception("Updated entries must be 1 to 1 map from input list")

        // Perform the update
        val (sql, updated) = update(entry, previous)
        if (!updated) throw new Exception("Entry not updated!")
        sql
    }

    retList
  }

  /**
   * Lock and update a model entry.
   * 
   * @param existing            Entry to be updated
   * @param acquireCondition    Condition that must be true at acquisition and false at release.
   * @param fun                 Update logic to be performed during lock, transforms acquired entry to updated entry
   * @return                    Entry that results from update
   */
  def exclusiveUpdate(existing: SqlType, acquireCondition: SqlType => Boolean)(fun: SqlType => SqlType): SqlType = {
    // Wraps/unwraps in list for special case of a single update
    val result = exclusiveUpdate(List(existing), acquireCondition) { list =>
      List(fun(list.head))
    }
    result.head
  }
}

object SquerylModel {
  import org.squeryl.dsl.ast.{ LogicalBoolean, BinaryOperatorNodeLogicalBoolean }

  /**
   *  Common logic for dynamically combining multiple squeryl expressions (with and)
   * @param exps    List of squeryl expressions
   * @return        Expression that results from intersection of input expressions
   */
  def combineExpressions(exps: List[LogicalBoolean], matchAll: Boolean = true) = {
    exps.length match {
      // TODO: skip roundtrip to DB when matchAll == false
      case 0 => true === matchAll
      case _ =>
        exps.reduceLeft { (a, b) =>
          new BinaryOperatorNodeLogicalBoolean(a, b, "and")
        }
    }
  }

  implicit def expressionAnder(exps: List[LogicalBoolean]): LogicalBoolean = combineExpressions(exps)

  class FilterStars[A](o: Option[A]) {
    def asParam[B](f: A => B): Option[B] = {
      if (!o.isDefined || o.get == "*") None
      else Some(f(o.get))
    }
  }
  implicit def makeAsParam[A](o: Option[A]): FilterStars[A] = new FilterStars(o)
}
