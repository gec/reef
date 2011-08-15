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

/**
 * Interface for CRUD model operations
 */
trait ModelCrud[A] {

  /**
   * Create a model entry
   *
   * @param entry   Object to be created
   * @return        Result of store creation/insertion
   */
  def create(context: RequestContext, entry: A): A

  /**
   * Update an existing model entry
   *
   * @param entry       Object to replace existing entry
   * @param existing    Existing entry to be replaced
   * @return            Result stored in data base and whether it was modified
   */
  def update(context: RequestContext, entry: A, existing: A): (A, Boolean)

  /**
   * Delete an existing entry
   *
   * @param entry       Existing entry to be deleted
   * @return            Result of store delete
   */
  def delete(context: RequestContext, entry: A): A
}

/**
 * Hooks/callbacks for modifying behavior without
 *  reimplementing generic CRUD operations
 */
trait ModelHooks[A] {

  /**
   * Called before create
   * @param entry   Object to be created
   * @return        Verified/modified object
   */
  protected def preCreate(context: RequestContext, entry: A): A = entry

  /**
   * Called after successful create
   * @param entry   Created entry
   */
  protected def postCreate(context: RequestContext, entry: A): Unit = {}

  /**
   * Called before update
   * @param entry       Object to replace existing entry
   * @param existing    Existing entry to be replaced
   * @return            Verified/modified object
   */
  protected def preUpdate(context: RequestContext, entry: A, existing: A): A = entry

  /**
   * Called after successful update
   * @param entry       Updated (current) entry
   * @param previous    Previous entry
   */
  protected def postUpdate(context: RequestContext, entry: A, previous: A): Unit = {}

  /**
   * Called before delete
   * @param entry       Existing entry to be deleted
   */
  protected def preDelete(context: RequestContext, entry: A): Unit = {}

  /**
   * Called after successful delete
   * @param previous    Previous entry
   */
  protected def postDelete(context: RequestContext, previous: A): Unit = {}

  /**
   * checks whether a new entry that is going to override a current entry
   * has actually modified that entry (to avoid unnecssary writes/events and
   * so we can send back correct status code
   */
  def isModified(entry: A, previous: A): Boolean
}

/**
 * Interface for observing model changes
 */
trait ModelObserver[A] {
  protected def onCreated(context: RequestContext, entry: A): Unit
  protected def onUpdated(context: RequestContext, entry: A): Unit
  protected def onDeleted(context: RequestContext, entry: A): Unit
}
