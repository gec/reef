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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.Model.{ Entity => EntityProto, EntityEdge => EntityEdgeProto, Relationship }
import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.ast.LogicalBoolean

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.exception.BadRequestException

import SquerylModel._
import java.util.UUID
import org.totalgrid.reef.models.{ EntityTypeMetaModel, ApplicationSchema, Entity, EntityEdge => Edge, EntityDerivedEdge => Derived, EntityToTypeJoins }
import org.totalgrid.reef.services.NullRequestContext
import com.weiglewilczek.slf4s.Logging

// implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._
import scala.collection.JavaConversions._

object EntityQuery extends Logging {

  import ApplicationSchema._

  /**
   * Entity-to-entity relationship.
   */
  case class Relate(rel: String, descendantOf: Boolean, dist: Int)

  /**
   * Tree node of entity query results, wraps an Entity (this node) with its
   * relationships to subnodes.
   */
  case class ResultNode(val ent: Entity, val subNodes: Map[Relate, List[ResultNode]]) {
    def id = ent.id
    def name = ent.name

    def types = {
      if (_types == None) {
        _types = Some(ent.types.value.toList)
      }
      _types.get
    }
    def types_=(l: List[String]) = { _types = Some(l) }
    protected var _types: Option[List[String]] = None

    def traverse[A](getEntry: ResultNode => A, f: ResultNode => Boolean): List[A] = {
      val subIds = for ((rel, nodes) <- subNodes; node <- nodes) yield node.traverse(getEntry, f)
      val list = subIds.toList.flatten

      if (f(this))
        getEntry(this) :: list
      else
        list
    }

    /**
     * Traverses tree for a flat list of all ids of entities of a certain type.
     */
    def idsForType(typ: String): List[UUID] = {
      traverse(_.id, _.types.contains(typ))
    }

    /**
     * Traverses tree for a flat list of all ids of returned entities
     */
    def flatIds(): List[UUID] = {
      traverse(_.id, x => true)
    }

    def flatEntites(): List[Entity] = {
      traverse(_.ent, x => true)
    }

    /**
     * Traverses tree to create Entity/Relationship proto tree.
     */
    def toProto: EntityProto = {
      val b = EntityProto.newBuilder.setUuid(makeUuid(ent)).setName(ent.name)
      types.foreach(b.addTypes(_))
      for ((rel, nodes) <- subNodes) {
        val r = Relationship.newBuilder
          .setRelationship(rel.rel)
          .setDescendantOf(rel.descendantOf)
          .setDistance(rel.dist)

        nodes.foreach(n => r.addEntities(n.toProto))
        b.addRelations(r)
      }
      b.build
    }

    /**
     * Visualizes the result tree.
     */
    def prettyPrint(indent: String): String = {
      val in = indent + "  "
      var str = "\n" + indent + "Entity { \n"
      str += in + "id: " + ent.id + "\n"
      str += in + "name: " + ent.name + "\n"
      str += in + ent.types.value.map("type: " + _).mkString("\n" + in)
      for ((rel, nodes) <- subNodes) {
        val in2 = in + "  "
        str += "\n"
        str += in + "Rel { \n"
        str += in2 + "rel: " + rel.rel + "\n"
        str += in2 + "descOf: " + rel.descendantOf + "\n"
        str += in2 + "dist: " + rel.dist + "\n"
        str += nodes.map(_.prettyPrint(in2)).mkString("")
        str += "\n" + in + "} \n"
      }

      str += "\n" + indent + "} \n"
      str
    }
    override def toString = {
      prettyPrint("")
    }
  }

  /**
   * Builder for assembling a result tree from query node tree.
   */
  class ResultNodeBuilder(val ent: Entity) {
    protected var subs = Map[Relate, List[ResultNodeBuilder]]()
    def addSubNode(rel: Relate, node: ResultNodeBuilder) = {
      subs += (rel -> (node :: (subs.get(rel) getOrElse Nil)))
    }

    def id = ent.id
    def build: ResultNode = ResultNode(ent, subs.mapValues(_.map(_.build)))
  }

  /**
   * Represents a subtree of the specifications of sets of subnodes. Specifications
   * consist of relationship information and partial descriptions of sub-entities.
   */
  case class QueryNode(
      val rel: Option[String],
      val descendantOf: Option[Boolean],
      val dist: Option[Int],
      val name: Option[String],
      val types: List[String],
      val subQueries: List[QueryNode]) {

    /**
     * Given an upper set of entities, find the lower set specified by this query node,
     * and recurse.
     *
     * @param upperQuery Squeryl query that represents upper set
     * @param upperNodes Set of mutable result tree-nodes to be filled out
     */
    def fillChildren(upperQuery: Query[Entity], upperNodes: List[ResultNodeBuilder]): Unit = {

      // short circuit the queries if we have no parent nodes
      if (upperNodes.isEmpty) return

      val entEdges = lowerQuery(upperNodes.map { _.id })
      val entsOnlyQuery = from(entEdges)(entEdge => select(entEdge._1))

      val ids = upperNodes.map(_.id)
      if (ids != ids.distinct) throw new Exception("Tree is not unique, same node has multiple links to itself, check model.")

      val upperMap = upperNodes.map(n => (n.id, n)).toMap

      val nodes = entEdges.map {
        case (ent, edge) =>
          val rel = Relate(edge.relationship, edge.childId == ent.id, edge.distance)
          val upperId = if (edge.childId == ent.id) edge.parentId else edge.childId
          val node = new ResultNodeBuilder(ent)
          upperMap(upperId).addSubNode(rel, node)
          node
      }.toList

      subQueries.foreach(sub => sub.fillChildren(entsOnlyQuery, nodes))
    }

    protected def lowerQuery(upperIds: List[UUID]) = {
      from(entities, edges)((lowEnt, edge) =>
        where(expr(lowEnt, edge, upperIds))
          select ((lowEnt, edge)))
    }

    protected def expr(ent: Entity, edge: Edge, upperIds: List[UUID]): LogicalBoolean = {

      val optList: List[Option[LogicalBoolean]] = List(name.map(ent.name === _),
        (types.size > 0) thenGet (ent.id in entityIdsFromTypes(types)),
        rel.map(edge.relationship === _),
        dist.map(edge.distance === _))

      def childQ = (ent.id === edge.childId) and (edge.parentId in upperIds)
      def parentQ = (ent.id === edge.parentId) and (edge.childId in upperIds)

      val foreignKey = descendantOf match {
        case Some(true) => childQ
        case Some(false) => parentQ
        case None => childQ or parentQ
      }

      foreignKey :: optList.flatten
    }
  }

  implicit def queryNodeToList(node: QueryNode): List[QueryNode] = List(node)

  /**
   * Executes a recursive search to go from a set of query trees to a set of result trees.
   *
   * @param queries Query tree root nodes
   * @param rootSelect Squeryl/sql select that represents the root set
   * @return Result tree root nodes (maps to rootSet) filled out by query
   */
  def resultsForQuery(queries: List[QueryNode], rootSelect: Query[Entity]): List[ResultNode] = {
    resultsForQuery(queries, rootSelect.toList, rootSelect)
  }

  /**
   * Executes a recursive search to go from a set of query trees to a set of result trees.
   *
   * @param queries Query tree root nodes
   * @param rootSet Entities from root query
   * @param rootSelect Squeryl/sql select that represents the root set
   * @return Result tree root nodes (maps to rootSet) filled out by query
   */
  def resultsForQuery(queries: List[QueryNode], rootSet: List[Entity], rootSelect: Query[Entity]): List[ResultNode] = {
    val results = rootSet.map(new ResultNodeBuilder(_)).toList
    queries.foreach(_.fillChildren(rootSelect, results))
    results.map(_.build)
  }

  /**
   * Translates a proto entity tree query to the internal, useful
   * representation.
   *
   * @param proto Proto representation of a entity tree query
   * @return List of tree subqueries.
   */
  def protoToQuery(proto: EntityProto): List[QueryNode] = {
    def buildSubQuery(rel: Relationship): List[QueryNode] = {
      if (rel.getEntitiesCount > 0) {
        rel.getEntitiesList.map { ent =>
          val subs = ent.getRelationsList.flatMap(buildSubQuery(_)).toList
          QueryNode(rel.relationship, rel.descendantOf, rel.distance, ent.name, ent.getTypesList.toList, subs)
        }.toList
      } else {
        QueryNode(rel.relationship, rel.descendantOf, rel.distance, None, Nil, Nil)
      }
    }

    proto.getRelationsList.flatMap(buildSubQuery(_)).toList
  }

  /**
   * Interprets a proto object as a entity tree query, gets the root set of
   * entities, and retrieves the query results.
   *
   * @param proto Proto representation of a entity tree query
   * @return List of root nodes representing result trees.
   */
  def protoTreeQuery(proto: EntityProto): List[ResultNode] = {

    // For the moment not allowing a root set of everything
    if (proto.uuid.value == None && proto.name == None && proto.getTypesCount == 0)
      throw new BadRequestException("Must specify root set")

    def expr(ent: Entity, typ: EntityToTypeJoins) = {
      proto.uuid.value.map(ent.id === UUID.fromString(_)) ::
        proto.name.map(ent.name === _) ::
        ((proto.getTypesCount > 0) thenGet ((typ.entType in proto.getTypesList.toList)
          and (typ.entityId === ent.id))) ::
          Nil
    }

    // If query specifies type, do a join, otherwise simpler query on id/name
    val rootQuery = if (proto.getTypesCount != 0) {
      from(entities, entityTypes)((ent, typ) =>
        where(expr(ent, typ).flatten)
          select (ent)).distinct
    } else {
      from(entities)(ent =>
        where((proto.uuid.value.map(ent.id === UUID.fromString(_)) ::
          proto.name.map(ent.name === _) :: Nil).flatten)
          select (ent))
    }

    // Execute query (unless root set is nil)
    if (rootQuery.size == 0) Nil
    else resultsForQuery(protoToQuery(proto), rootQuery)
  }

  /**
   * Return all ids of a certain entity type retrieved by an entity query.
   *
   * @param proto Proto representation of a entity tree query
   * @param typ Entity type to return ids of
   * @return List of entity ids from entity query of specified type
   */
  def typeIdsFromProtoQuery(proto: EntityProto, typ: String): List[UUID] = {
    protoTreeQuery(proto).flatMap(_.idsForType(typ))
  }

  /**
   * Return a list of descendants of the entity aUUID with this entity.
   */
  def idsFromProtoQuery(proto: EntityProto): List[UUID] = {
    protoTreeQuery(proto).flatMap(_.flatIds())
  }

  def minimalEntityToProto(entry: Entity): EntityProto.Builder = {
    EntityProto.newBuilder.setUuid(makeUuid(entry)).setName(entry.name)
  }

  def entityToProto(entry: Entity): EntityProto.Builder = {
    val b = minimalEntityToProto(entry)
    entry.types.value.foreach(t => b.addTypes(t))
    b
  }

  // All entities as list, no relationships
  def allQuery: List[Entity] = {
    entities.where(t => true === true).toList
  }

  def entityIdsFromTypes(types: List[String]) = {
    from(entityTypes)(typ =>
      where(typ.entType in types)
        select (typ.entityId))
  }

  def noneIfEmpty(listO: Option[List[String]]) = {
    if (listO.isEmpty || listO.get.size == 0) None else listO
  }

  def returnSingleOption[A](o: List[A], what: String): Option[A] = {
    if (o.size > 1) throw new Exception(what + " does not exist")
    if (o.size == 1) Some(o.head) else None
  }

  def findEntity(proto: EntityProto): Option[Entity] = {
    if (proto.hasUuid) {
      returnSingleOption(entities.where(t => t.id === UUID.fromString(proto.getUuid.getValue)).toList, "Entity")
    } else if (proto.hasName) {
      returnSingleOption(entities.where(t => t.name === proto.getName).toList, "Entity")
    } else {
      throw new Exception("Not valid query")
    }
  }

  def getChildren(rootId: UUID, relation: String) = {
    from(entities)(ent =>
      where(ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId)))
        select (ent))
  }

  def getParents(rootId: UUID, relation: String) = {
    from(entities)(ent =>
      where(ent.id in
        from(edges)(edge =>
          where((edge.childId === rootId) and (edge.relationship === relation))
            select (edge.parentId)))
        select (ent))
  }

  def getParentsWithDistance(rootId: UUID, relation: String) = {
    from(entities, edges)((ent, edge) =>
      where(ent.id === edge.parentId and edge.childId === rootId and edge.relationship === relation)
        select (ent, edge.distance))
  }

  def getChildrenWithDistance(rootId: UUID, relation: String) = {
    from(entities, edges)((ent, edge) =>
      where(ent.id === edge.childId and edge.parentId === rootId and edge.relationship === relation)
        select (ent, edge.distance))
  }

  // Helper for 
  def getChildrenOfType(rootId: UUID, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent))
  }

  def getParentOfType(rootId: UUID, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.childId === rootId) and (edge.relationship === relation))
            select (edge.parentId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent))
  }

  def findEdge(proto: EntityEdgeProto): Option[Edge] = {
    proto.uuid.value.flatMap { v =>
      returnSingleOption(edges.where(t => t.id === v.toInt).toList, "Entity Edge")
    }
  }

  def findEntitiesByType(types: List[String]) = {
    from(entities, entityTypes)((ent, typ) =>
      where(typ.entityId === ent.id and (typ.entType in types))
        select (ent)).distinct
  }
  def findEntities(names: List[String], types: List[String]) = {
    from(entities, entityTypes)((ent, typ) =>
      where(ent.name in names and
        typ.entityId === ent.id and (typ.entType in types))
        select (ent)).distinct
  }

  def findEntityIds(names: List[String], types: List[String]) = {
    from(entities, entityTypes)((ent, typ) =>
      where(ent.name in names and
        typ.entityId === ent.id and (typ.entType in types))
        select (ent.id)).distinct
  }

  def entityIdsFromType(typ: String) = {
    from(entityTypes)(t =>
      where(t.entType === typ)
        select (t.entityId))
  }

  def findIdsOfChildren(rootNode: EntityProto, relation: String, childType: String): Query[UUID] = {
    if (rootNode.uuid.value == Some("*") || rootNode.name == Some("*")) {
      entityIdsFromType(childType)
    } else {
      // TODO: get entitiy queries to use and respect requestContext - backlog-70
      EntitySearches.findRecord(new NullRequestContext, rootNode).map { rootEnt =>
        from(getChildrenOfType(rootEnt.id, relation, childType))(ent => select(ent.id))
      }.getOrElse(from(entities)(e => where(true === false) select (e.id)))
    }
  }

}

trait EntitySearches extends UniqueAndSearchQueryable[EntityProto, Entity] {
  val table = ApplicationSchema.entities
  def uniqueQuery(proto: EntityProto, sql: Entity) = {
    List(
      proto.uuid.value.asParam(sql.id === UUID.fromString(_)),
      proto.name.asParam(sql.name === _),
      EntityQuery.noneIfEmpty(proto.types).asParam(sql.id in EntityQuery.entityIdsFromTypes(_)))
  }

  def searchQuery(proto: EntityProto, sql: Entity) = Nil
}
object EntitySearches extends EntitySearches

case class EntitySearch(uuid: Option[String], name: Option[String], types: Option[List[String]]) {
  def map[B](f: EntitySearch => B): Option[B] = {
    if (uuid.isDefined || name.isDefined || (types.isDefined && !types.get.isEmpty)) Some(f(this))
    else None
  }
}

trait EntityPartsSearches extends UniqueAndSearchQueryable[EntitySearch, Entity] {
  val table = ApplicationSchema.entities
  def uniqueQuery(proto: EntitySearch, sql: Entity) = {
    List(
      proto.uuid.asParam(sql.id === UUID.fromString(_)),
      proto.name.asParam(sql.name === _),
      EntityQuery.noneIfEmpty(proto.types).asParam(sql.id in EntityQuery.entityIdsFromTypes(_)))
  }

  def searchQuery(proto: EntitySearch, sql: Entity) = Nil
}
object EntityPartsSearches extends EntityPartsSearches

