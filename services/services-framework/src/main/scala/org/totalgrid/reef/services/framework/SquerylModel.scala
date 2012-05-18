/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.framework

import org.squeryl.PrimitiveTypeMode._
import com.google.protobuf.GeneratedMessage
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess._
import org.squeryl.Table
import org.totalgrid.reef.models.ModelWithIdBase

/**
 * Supertype for Proto/Squeryl models
 */
trait SquerylServiceModel[SqlKeyType, ServiceType <: GeneratedMessage, SqlType <: ModelWithIdBase[SqlKeyType]]
    extends ServiceModel[ServiceType, SqlType]
    with BasicSquerylModel[SqlKeyType, SqlType] { self: ModelObserver[SqlType] =>
}

/**
 * Interface for Squeryl-typed CRUD operations
 */
trait SquerylModel[SqlKeyType, SqlType <: ModelWithIdBase[SqlKeyType]] extends ModelCrud[SqlType]

/**
 * Implementation of CRUD (with lifecycle hooks) for Squeryl backend. Concrete classes
 * must provide Squeryl table reference as well as an implementation of ModelObserver
 */
trait BasicSquerylModel[SqlKeyType, SqlType <: ModelWithIdBase[SqlKeyType]]
    extends SquerylModel[SqlKeyType, SqlType]
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
  def create(context: RequestContext, entry: SqlType): SqlType = {
    val sql = preCreate(context, entry)
    val inserted = table.insert(sql)
    onCreated(context, inserted)
    postCreate(context, inserted)
    inserted
  }

  /**
   * Update an existing model entry
   *
   * @param entry       Object to replace existing entry
   * @param existing    Existing entry to be replaced
   * @return            Result stored in data base and whether it was modified
   */
  def update(context: RequestContext, entry: SqlType, existing: SqlType): (SqlType, Boolean) = {
    val sql = preUpdate(context, entry, existing)
    if (isModified(entry, existing)) {
      sql.id = existing.id
      table.update(sql)
      onUpdated(context, sql)
      postUpdate(context, sql, existing)
      (sql, true)
    } else {
      logger.debug(entry + " not modified")
      (existing, false)
    }
  }

  /**
   * Delete an existing entry
   *
   * @param entry       Existing entry to be deleted
   * @return            Result of store delete
   */
  def delete(context: RequestContext, entry: SqlType): SqlType = {
    preDelete(context, entry)
    table.delete(entry.id)
    onDeleted(context, entry)
    postDelete(context, entry)
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
  def exclusiveUpdate(context: RequestContext, existing: List[SqlType], acquireCondition: SqlType => Boolean)(fun: List[SqlType] => List[SqlType]): List[SqlType] = {

    val ids = existing.map(_.id)

    // apparently these selects can return differently ordered lists
    // Select for update
    val objList = table.where(c => c.getIn(ids)).forUpdate.toList
    // we need two copies, one for modifying and one we old onto to check for changes
    val list = table.where(c => c.getIn(ids)).toList

    // Fail if we have nothing
    if (objList.size < ids.size) throw new ObjectMissingException("Cannot find objects with ids: " + ids)

    // Precondition on all objects
    if (objList.exists(!acquireCondition(_))) throw new AcquireConditionNotMetException("Not all objects have correct condition.")

    // reorder the results back into the passed in order
    def originalOrder(l: List[SqlType]) = {
      val lookup = l.map { e => e.id -> e }.toMap
      ids.map(lookup(_))
    }

    // Get results, do any work inside the lock
    val results = fun(originalOrder(objList))

    if (results.length != objList.length) throw new InvalidUpdateException("Updated entries must be 1 to 1 map from input list")

    // Postcondition on all objects
    if (results.exists(acquireCondition(_))) throw new AcquireConditionStillValidException("Not all entries have updated the necessary fields")

    // Do the update, get the list of actual rows (after model hooks)
    val retList = results.zip(originalOrder(list)).map {
      case (entry, previous) =>
        // Assert this is the same table row
        if (entry.id != previous.id)
          throw new InvalidUpdateException("Updated entries must be in same order as input list")

        // Perform the update
        val (sql, updated) = update(context, entry, previous)
        if (!updated) throw new InvalidUpdateException("Entry not updated!")
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
  def exclusiveUpdate(context: RequestContext, existing: SqlType, acquireCondition: SqlType => Boolean)(fun: SqlType => SqlType): (SqlType, Boolean) = {
    // Wraps/unwraps in list for special case of a single update
    val result = exclusiveUpdate(context, List(existing), acquireCondition) { list =>
      val result = fun(list.head)
      // set the id of the updated entry to match original entry
      result.id = list.head.id
      List(result)
    }
    // will have updated or else thrown exception
    (result.head, true)
  }
}

object SquerylModel {

  import org.squeryl.dsl.ast.LogicalBoolean

  /**
   * instead of directly using squeryl's LogicalBoolean type we'll wrap it in a case class we
   * can then use to "short-circuit" a search when we have been given a term like UUID.
   */
  case class SearchTerm(operand: LogicalBoolean, isUnique: Boolean)

  class AsUnique(o: Option[LogicalBoolean]) {
    def unique: Option[SearchTerm] = {
      if (o.eq(WILDCARD)) o.map { SearchTerm(_, false) }
      else o.map { SearchTerm(_, true) }
    }
    def search: Option[SearchTerm] = {
      o.map { SearchTerm(_, false) }
    }
  }
  implicit def makeAsNormal(o: LogicalBoolean): SearchTerm = SearchTerm(o, false)
  implicit def makeAsUniqueO(o: Option[LogicalBoolean]): AsUnique = new AsUnique(o)
  implicit def makeAsNormalO(o: Option[LogicalBoolean]): Option[SearchTerm] = o.map { SearchTerm(_, false) }

  implicit def makeAsNormalList(list: List[Option[LogicalBoolean]]): List[Option[SearchTerm]] = list.map { makeAsNormalO(_) }

  implicit def unMakeAsNormal(o: SearchTerm): LogicalBoolean = o.operand
  implicit def unMakeAsNormal(o: Option[SearchTerm]): Option[LogicalBoolean] = o.map { _.operand }

  implicit def convertSearchTerms(exps: List[SearchTerm]): List[LogicalBoolean] = {
    exps.find(_.isUnique) match {
      case Some(op) => List(op.operand)
      case None => exps.map { _.operand }
    }
  }

  /**
   * we use this singleton to indicate a wildcard search so we can differentiate between a totally blank query and
   * one where we asked for a wildcard search on some parameter
   */
  private val WILDCARD: Option[LogicalBoolean] = Some(true === true)

  class FilterStars[A](o: Option[A]) {
    def asParam(f: A => LogicalBoolean): Option[LogicalBoolean] = {
      o match {
        case None => None
        case Some("*") => WILDCARD
        case _ => Some(f(o.get))
      }
    }
  }
  implicit def makeAsParam[A](o: Option[A]): FilterStars[A] = new FilterStars(o)

  class ListFilterStars[A](list: Option[List[A]]) {
    def asParam(f: List[A] => LogicalBoolean): Option[LogicalBoolean] = {
      list match {
        case None => None
        case Some(List()) => None
        case Some(List("*")) => WILDCARD
        case Some(l) => Some(f(l))
      }
    }
  }
  implicit def makeListAsParam1[A](list: List[A]): ListFilterStars[A] = new ListFilterStars(Some(list))
  import scala.collection.JavaConversions._
  implicit def makeListAsParam2[A](javaList: java.util.List[A]): ListFilterStars[A] = new ListFilterStars(Some(javaList.toList))

  implicit def makeListAsParam3[A](list: Option[List[A]]): ListFilterStars[A] = new ListFilterStars(list)
  implicit def makeListAsParam4[A](javaList: Option[java.util.List[A]]): ListFilterStars[A] = new ListFilterStars(javaList.map { _.toList })

  def routingOption[A, R](jList: java.util.List[A])(f: List[A] => List[R]): List[R] = {
    import scala.collection.JavaConversions._
    routingOption(jList.toList)(f)
  }

  def routingOption[A, R](list: List[A])(f: List[A] => List[R]): List[R] = {
    list match {
      case List() => Nil
      case List("*") => Nil
      case _ => f(list)
    }
  }

  def createSubscriptionPermutations(lists: List[List[String]]): List[List[Option[String]]] = {
    lists.foldLeft(List(Nil: List[Option[String]])) { (keys, entries) =>
      if (entries.isEmpty) keys.map { k => None :: k }
      else entries.map { e => keys.map { k => Some(e) :: k } }.flatten
    }.map { _.reverse }
  }
}
