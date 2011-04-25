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
package org.totalgrid.reef.models

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

import java.util.UUID

class ActiveModelException(msg: String) extends Exception(msg)

trait ActiveModel {
  def hasOne[A <: KeyedEntity[Long]](table: Table[A], id: Long): A = {
    table.lookup(id) match {
      case Some(s) => s
      case None =>
        throw new ActiveModelException("Missing id: " + id + " in " + table)
    }
  }

  def mayHaveOne[A <: KeyedEntity[Long]](table: Table[A], optId: Option[Long]): Option[A] = {
    optId match {
      case Some(-1) => None
      case Some(id) => Some(hasOne(table, id))
      case None => None
    }
  }

  def mayHaveOne[A](query: Query[A]): Option[A] = {
    query.toList match {
      case List(x) => Some(x)
      case _ => None
    }
  }

  def mayBelongTo[A](query: Query[A]): Option[A] = {

    query.size match {
      case 1 => Some(query.single)
      case _ => None
    }
  }

  def belongTo[A](query: Query[A]): A = {

    query.size match {
      case 1 => query.single
      case _ => throw new ActiveModelException("Missing belongTo relation")
    }
  }
}

trait ModelWithId extends KeyedEntity[Long] with ActiveModel {
  var id: Long = 0

}

trait ModelWithUUID extends KeyedEntity[UUID] with ActiveModel {
  var id: UUID

}