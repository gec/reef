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
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models.SquerylConversions

import SquerylModel._
import org.totalgrid.reef.client.operations.scl.ScalaRequestHeaders._
import org.totalgrid.reef.authz.VisibilityMap

/**
 * defines a simple to integrate implementation of the findRecord and findRecords functions
 * that makes a clear and explicit distinction between "identity"/"unique" fields and "data"
 * fields.
 */
trait UniqueAndSearchQueryable[MessageType, T] {

  /**
   * table all of the queries are run against
   */
  def table: Table[T]

  /**
   * limit results to stop denial of service
   */
  def getResultLimit(context: RequestContext) = {
    val limit = context.getHeaders.resultLimit.getOrElse(100)
    if (limit < 0) throw new BadRequestException("RESULT_LIMIT header needs to be integer larger than 0")
    limit
  }

  /**
   * to enforce an order on the returned rows override this function with something like:
   *  select.orderBy(new OrderByArg(sql.time).asc)
   * By default we apply no ordering (usually is primary key which is close to insertion order)
   */
  def getOrdering[R](select: SelectState[R], sql: T): QueryYield[R] = select

  /**
   *  client code need to return a list that has all of the fields necessary to determine
   * if 2 records are referring to the "same object", it may be one field or a combination
   * of all the fields to define the "sameness". In most cases a id field will be in this list
   * and by definition if that field is filled in we will get that record and no others. If
   * however the model was a one where every user could set a user_specific_status for every command
   * then the search would look like: List(user_name, command_name). To make a unique match the
   * client would have to have specified both a user_name and command_name. If however the client
   * only specified one of the two fields (lets assume user_name) then we would return all of the
   * entries that had that matching field (user_name). Since these uniqueQueries are useful for
   * searching they are merged with the searchQueries to avoid code duplication.
   */
  def uniqueQuery(context: RequestContext, proto: MessageType, sql: T): List[Option[SearchTerm]]

  /**
   * this list is for fields that we want to be searchable but do not factor into determining
   * resource identity. Expanding on the example used in uniqueQuery, the user_specific_status
   * would be a likely searchable filed. That way the client could search by user_specific_status
   * across all users/commands but if they attempt to create a new status we would be able to determine
   * which record we should be updating.
   */
  def searchQuery(context: RequestContext, proto: MessageType, sql: T): List[Option[SearchTerm]]

  /**
   * pre-filter our sql query so we only ever pull "visible" items from the database and therefore get the
   * correct ordering/limit behaviors. If we can see all of the entries return a tautology (true === true)
   */
  def selector(map: VisibilityMap, sql: T): LogicalBoolean

  /**
   * helper function for use in complex queries in models :
   * proto.point.map(pointProto => sql.pointId in PointServiceConversion.searchQueryForId(pointProto, { _.id }))
   * @param idFun this function determines which field we are trying to match against (not always .id)
   */
  def uniqueQueryForId[R](context: RequestContext, req: MessageType, idFun: T => R): Query[R] = {
    from(table)(sql => where(uniqueParams(context, req, sql)) select (idFun(sql)))
  }

  /**
   * same as uniqueQueryForId but using the broader searching
   */
  def searchQueryForId[R](context: RequestContext, req: MessageType, idFun: T => R): Query[R] = {
    from(table)(sql => where(searchParams(context, req, sql)) select (idFun(sql)))
  }

  /**
   * implement the MessageModelConversion interface to use the uniqueQuery to
   * find a single record for updating/creating
   */
  def findRecord(context: RequestContext, messageType: MessageType): Option[T] = {
    val uniqueItems = doUniqueQuery(context, messageType, { (sql, w) => w.select(sql) }, getResultLimit(context)).toList
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
  def findRecords(context: RequestContext, req: MessageType): List[T] = {
    doSearchQuery(context, req, { (sql, w) => w.select(sql) }, getResultLimit(context)).toList
  }

  /**
   * returns the length of non-blank, non-wildcard query parameters in the unique query, this is useful to
   * determine whether searching for an object is more specific than "get all" for switching on default
   * behaviors.
   */
  def uniqueQuerySize(context: RequestContext, req: MessageType): Int = {
    // TODO: remove uniqueQuerySize and searchQuerySize functions
    // we are searching for, in some cases we need to know if they are searching for something in paticular or if they
    // searched for "*" (this would be the same as searching for nothing)
    from(table)(sql => where(return uniqueQuery(context, req, sql).flatten.size) select (sql))
    throw new Exception
  }
  /**
   * same as searchQuerySize but using the broader searching
   */
  def searchQuerySize(context: RequestContext, req: MessageType): Int = {
    from(table)(sql => where(return (uniqueQuery(context, req, sql) ::: searchQuery(context, req, sql)).flatten.size) select (sql))
    throw new Exception
  }

  /// internal (for now) functions that minimize code duplication but aren't needed externally yet
  /// though as the system grows that may change
  private def doUniqueQuery[R](context: RequestContext, req: MessageType, selectFun: (T, WhereState[Conditioned]) => SelectState[R], resultLimit: Int): Query[R] = {
    from(table)(sql => selectFun(sql, where(uniqueParams(context, req, sql)))).page(0, resultLimit)
  }
  private def doSearchQuery[R](context: RequestContext, req: MessageType, selectFun: (T, WhereState[Conditioned]) => SelectState[R], resultLimit: Int): Query[R] = {
    from(table)(sql => getOrdering(selectFun(sql, where(searchParams(context, req, sql))), sql)).page(0, resultLimit)
  }

  private def filterForVisiblity(context: RequestContext, sql: T, request: LogicalBoolean): LogicalBoolean = {
    SquerylConversions.combineExpressions(List(request, selector(context.auth.visibilityMap(context), sql)))
  }

  private def uniqueParams(context: RequestContext, req: MessageType, sql: T): LogicalBoolean = {
    filterForVisiblity(context, sql, SquerylConversions.combineExpressions(uniqueQuery(context, req, sql).flatten))
  }
  private def searchParams(context: RequestContext, req: MessageType, sql: T): LogicalBoolean = {
    filterForVisiblity(context, sql, SquerylConversions.combineExpressions((uniqueQuery(context, req, sql) ::: searchQuery(context, req, sql)).flatten))
  }
}