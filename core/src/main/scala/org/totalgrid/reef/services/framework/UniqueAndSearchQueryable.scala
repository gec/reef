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

import org.squeryl._
import dsl.ast.LogicalBoolean
import dsl.fsm.{ Conditioned, SelectState, WhereState }
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.QueryYield

/**
 * defines a simple to integrate implementation of the findRecord and findRecords functions
 * that makes a clear and explicit distinction between "identity"/"unique" fields and "data"
 * fields.
 */
trait UniqueAndSearchQueryable[MessageType, T] {

  /**
   * table all of the queries are run against
   */
  val table: Table[T]

  /**
   * limit results to stop denial of service, TODO: make this per query overridable
   */
  def getResultLimit: Int = 100

  /**
   * to enforce an order on the returned rows override this function with something like:
   *  select.orderBy(new OrderByArg(sql.time).asc)
   * By default we apply no ordering (usually is primary key which is close to insertion order)
   */
  def getOrdering[R](select: SelectState[R], sql: T): QueryYield[R] = select

  /**
   *  client code need to return a list that has all of the fields necessary to determine
   * if 2 records are referring to the "same object", it may be one field or a combination
   * of all the fields to define the "sameness". In most cases a uid field will be in this list
   * and by definition if that field is filled in we will get that record and no others. If
   * however the model was a one where every user could set a user_specific_status for every command
   * then the search would look like: List(user_name, command_name). To make a unique match the
   * client would have to have specified both a user_name and command_name. If however the client
   * only specified one of the two fields (lets assume user_name) then we would return all of the
   * entries that had that matching field (user_name). Since these uniqueQueries are useful for
   * searching they are merged with the searchQueries to avoid code duplication.
   */
  def uniqueQuery(proto: MessageType, sql: T): List[Option[LogicalBoolean]]

  /**
   * this list is for fields that we want to be searchable but do not factor into determining
   * resource identity. Expanding on the example used in uniqueQuery, the user_specific_status
   * would be a likely searchable filed. That way the client could search by user_specific_status
   * across all users/commands but if they attempt to create a new status we would be able to determine
   * which record we should be updating.
   */
  def searchQuery(proto: MessageType, sql: T): List[Option[LogicalBoolean]]

  /**
   * helper function for use in complex queries in models :
   * proto.point.map(pointProto => sql.pointId in PointServiceConversion.searchQueryForId(pointProto, { _.id }))
   * @param idFun this function determines which field we are trying to match against (not always .id)
   */
  def uniqueQueryForId[R](req: MessageType, idFun: T => R): Query[R] = {
    from(table)(sql => where(uniqueParams(req, sql)) select (idFun(sql)))
  }

  /**
   * same as uniqueQueryForId but using the broader searching
   */
  def searchQueryForId[R](req: MessageType, idFun: T => R): Query[R] = {
    from(table)(sql => where(searchParams(req, sql)) select (idFun(sql)))
  }

  /**
   * implement the MessageModelConversion interface to use the uniqueQuery to
   * find a single record for updating/creating
   */
  def findRecord(req: MessageType): Option[T] = {
    val uniqueItems = uniqueQuery(req, { (sql, w) => w.select(sql) }).toList
    uniqueItems.size match {
      case 0 => None
      case 1 => Some(uniqueItems.head)
      case _ => throw new Exception("Unique query returned " + uniqueItems.size + " entries")
    }
  }

  /**
   * implement the MessageModelConversion interface to do a wildcard search for
   * all records matching the request proto
   */
  def findRecords(req: MessageType): List[T] = {
    searchQuery(req, { (sql, w) => w.select(sql) }).toList
  }

  /**
   * returns the length of non-blank, non-wildcard query parameters in the unique query, this is useful to
   * determine whether searching for an object is more specific than "get all" for switching on default
   * behaviors.
   */
  def uniqueQuerySize(req: MessageType): Int = {
    // TODO: HACK, this construction allows us to get a "free" instance of T to use to construct the list of query parameters
    // we are searching for, in some cases we need to know if they are searching for something in paticular or if they
    // searched for "*" (this would be the same as searching for nothing)
    from(table)(sql => where(return uniqueQuery(req, sql).flatten.size) select (sql))
    throw new Exception
  }
  /**
   * same as searchQuerySize but using the broader searching
   */
  def searchQuerySize(req: MessageType): Int = {
    from(table)(sql => where(return (uniqueQuery(req, sql) ::: searchQuery(req, sql)).flatten.size) select (sql))
    throw new Exception
  }

  /// internal (for now) functions that minimize code duplication but aren't needed externally yet
  /// though as the system grows that may change
  private def uniqueQuery[R](req: MessageType, selectFun: (T, WhereState[Conditioned]) => SelectState[R]): Query[R] = {
    from(table)(sql => selectFun(sql, where(uniqueParams(req, sql)))).page(0, getResultLimit)
  }
  private def searchQuery[R](req: MessageType, selectFun: (T, WhereState[Conditioned]) => SelectState[R]): Query[R] = {
    from(table)(sql => getOrdering(selectFun(sql, where(searchParams(req, sql))), sql)).page(0, getResultLimit)
  }
  def uniqueParams(req: MessageType, sql: T): LogicalBoolean = {
    SquerylModel.combineExpressions(uniqueQuery(req, sql).flatten)
  }
  def searchParams(req: MessageType, sql: T): LogicalBoolean = {
    SquerylModel.combineExpressions((uniqueQuery(req, sql) ::: searchQuery(req, sql)).flatten)
  }
}