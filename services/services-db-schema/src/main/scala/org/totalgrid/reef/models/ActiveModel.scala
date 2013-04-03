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
        throw new ActiveModelException("Missing id: " + id + " in " + table.name)
    }
  }
  def mayHaveOne[A <: KeyedEntity[Long]](table: Table[A], optId: Option[Long]): Option[A] = {
    optId match {
      case Some(-1) => None
      case Some(id) => Some(hasOne(table, id))
      case None => None
    }
  }

  def hasOneByUuid[A <: KeyedEntity[UUID]](table: Table[A], id: UUID): A = {
    table.lookup(id) match {
      case Some(s) => s
      case None =>
        throw new ActiveModelException("Missing id: " + id + " in " + table.name)
    }
  }
  def mayHaveOneByUuid[A <: KeyedEntity[UUID]](table: Table[A], optId: Option[UUID]): Option[A] = {
    optId match {
      case Some(id) => Some(hasOneByUuid(table, id))
      case None => None
    }
  }

  def hasOneByEntityUuid[A <: EntityBasedModel](table: Table[A], id: UUID): A = {
    table.where(_.entityId === id).headOption match {
      case Some(s) => s
      case None =>
        throw new ActiveModelException("Missing id: " + id + " in " + table.name)
    }
  }

  def mayHaveOneByEntityUuid[A <: EntityBasedModel](table: Table[A], optId: Option[UUID]): Option[A] = {
    optId match {
      case Some(id) => Some(hasOneByEntityUuid(table, id))
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

import org.squeryl.dsl.ast.LogicalBoolean

trait ModelWithIdBase[A] extends KeyedEntity[A] with ActiveModel {
  def id_=(v: A)

  def getIn(list: List[A]): LogicalBoolean
}

trait ModelWithId extends ModelWithIdBase[Long] {
  var id: Long = 0

  import org.squeryl.PrimitiveTypeMode._

  def getIn(list: List[Long]): LogicalBoolean = {
    id in list
  }
}

import java.util.UUID

/**
 * trait that allows us to mixin different UUID generation methods
 */
trait UUIDGenerator {
  def newUUID(): UUID = {
    UUID.randomUUID()
  }
}

trait ModelWithUUID extends ModelWithIdBase[UUID] with UUIDGenerator {
  var id: UUID = newUUID

  import org.squeryl.PrimitiveTypeMode._

  def getIn(list: List[UUID]): LogicalBoolean = {
    id in list
  }
}

class EntityBasedModel(val entityId: UUID) extends ModelWithId {
  import org.totalgrid.reef.util.LazyVar

  val entity = LazyVar(hasOneByUuid(ApplicationSchema.entities, entityId))

  def entityName = entity.value.name
}

object EntityBasedModel {
  def preloadEntities(entries: List[EntityBasedModel]) {
    val entitiesWithTypes = from(ApplicationSchema.entities, ApplicationSchema.entityTypes)((ent, typ) =>
      where(ent.id in entries.map { _.entityId } and (typ.entityId === ent.id))
        select (ent, typ.entType)).toList.groupBy(_._1.id)

    entries.foreach { entry =>
      val listOfEntityWithTypes = entitiesWithTypes(entry.entityId)
      val entity = listOfEntityWithTypes.head._1
      entry.entity.value = entity
      entity.types.value = listOfEntityWithTypes.map { _._2 }
    }
  }
}
