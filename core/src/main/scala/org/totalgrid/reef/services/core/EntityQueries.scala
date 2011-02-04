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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.proto.Model.{ Entity => EntityProto, Relationship }
import org.totalgrid.reef.models.{ ApplicationSchema, Entity, EntityEdge => Edge, EntityDerivedEdge => Derived, EntityToTypeJoins }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.ast.LogicalBoolean

import OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._

trait EntityTreeQueries { self: EntityQueries =>

  import ApplicationSchema._

  implicit def queryNodeToList(node: QueryNode): List[QueryNode] = List(node)

  /**
   * Executes a recursive search to go from a set of query trees to a set of result trees.
   *
   * @param queries Query tree root nodes
   * @param rootSet Entities from root query
   * @return Result tree root nodes (maps to rootSet) filled out by query
   */
  def resultsForQuery(queries: List[QueryNode], rootSet: List[Entity]): List[ResultNode] = {
    val select = from(entities)(t => where(t.id in rootSet.map(_.id)) select (t))
    resultsForQuery(queries, rootSet, select)
  }

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
    if (proto.uid == None && proto.name == None && proto.getTypesCount == 0)
      throw new Exception("Must specify root set")

    def expr(ent: Entity, typ: EntityToTypeJoins) = {
      proto.uid.map(ent.id === _.toLong) ::
        proto.name.map(ent.name === _) ::
        ((proto.getTypesCount > 0) thenGet ((typ.entType in proto.getTypesList.toList)
          and (typ.entityId === ent.id))) ::
        Nil
    }

    // If query specifies type, do a join, otherwise simpler query on uid/name
    val rootQuery = if (proto.getTypesCount != 0) {
      from(entities, entityTypes)((ent, typ) =>
        where(expr(ent, typ).flatten)
          select (ent)).distinct
    } else {
      from(entities)(ent =>
        where((proto.uid.map(ent.id === _.toLong) ::
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
  def typeIdsFromProtoQuery(proto: EntityProto, typ: String): List[Long] = {
    protoTreeQuery(proto).flatMap(_.idsForType(typ))
  }

  /**
   * Return a list of descendants of the entity along with this entity.
   */
  def idsFromProtoQuery(proto: EntityProto): List[Long] = {
    protoTreeQuery(proto).flatMap(_.flatIds())
  }

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

    def traverse(f: ResultNode => Boolean): List[Long] = {
      val subIds = for ((rel, nodes) <- subNodes; node <- nodes) yield node.traverse(f)
      val list = subIds.toList.flatten

      if (f(this))
        id :: list
      else
        list
    }

    /**
     * Traverses tree for a flat list of all ids of entities of a certain type.
     */
    def idsForType(typ: String): List[Long] = {
      traverse(_.types.contains(typ))
    }

    /**
     * Traverses tree for a flat list of all ids of returned entities
     */
    def flatIds(): List[Long] = {
      traverse(x => true)
    }

    /**
     * Traverses tree for a flat list of all ids of entities of a certain type.
     */
    /*def idsForType(typ: String): List[Long] = {
      val subIds = for ((rel, nodes) <- subNodes; node <- nodes) yield node.idsForType(typ)
      val list = subIds.toList.flatten

      if (types.contains(typ))
        id :: list
      else
        list
    }*/

    /**
     * Traverses tree to create Entity/Relationship proto tree.
     */
    def toProto: EntityProto = {
      val b = EntityProto.newBuilder.setUid(ent.id.toString).setName(ent.name)
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
      val entEdges = lowerQuery(upperQuery)
      val entsOnlyQuery = from(entEdges)(entEdge => select(entEdge._1))

      val ids = upperNodes.map(_.id)
      assert(ids == ids.distinct)

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

    protected def lowerQuery(upperQuery: Query[Entity]) = {
      from(entities, edges)((lowEnt, edge) =>
        where(expr(lowEnt, edge, upperQuery))
          select ((lowEnt, edge)))
    }

    protected def expr(ent: Entity, edge: Edge, upper: Query[Entity]): LogicalBoolean = {
      val upperIds = from(upper)(t => select(t.id))

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
}

trait EntityQueries extends EntityTreeQueries {

  val entities = ApplicationSchema.entities
  val edges = ApplicationSchema.edges
  val deriveds = ApplicationSchema.derivedEdges
  val entityTypes = ApplicationSchema.entityTypes

  def entityToProto(entry: Entity): EntityProto.Builder = {
    val b = EntityProto.newBuilder
      .setUid(entry.id.toString)
      .setName(entry.name)
    entry.types.value.foreach(t => b.addTypes(t))
    b
  }

  // All entities as list, no relationships
  def allQuery: List[EntityProto] = {
    entities.where(t => true === true).map(entityToProto(_).build).toList
  }

  // Find list of Entities matching type/name, no relationships
  def nameTypeQuery(name: Option[String], types: Option[List[String]]): List[Entity] = {
    import SquerylModel._

    from(entities)(t =>
      where(List(
        name.map(t.name === _),
        types.map(t.id in entityIdsFromTypes(_))).flatten)
        select (t)).distinct.toList
  }

  def entityIdsFromTypes(types: List[String]) = {
    from(entityTypes)(typ =>
      where(typ.entType in types)
        select (&(typ.entityId)))
  }

  def noneIfEmpty(listO: Option[List[String]]) = {
    if (listO.isEmpty || listO.get.size == 0) None else listO
  }

  def returnSingleOption[A](o: List[A]): Option[A] = {
    if (o.size > 1) throw new Exception("Entity does not exist")
    if (o.size == 1) Some(o.head) else None
  }

  def findEntity(proto: EntityProto): Option[Entity] = {
    if (proto.hasUid) {
      returnSingleOption(entities.where(t => t.id === proto.getUid.toLong).toList)
    } else if (proto.hasName) {
      returnSingleOption(entities.where(t => t.name === proto.getName).toList)
    } else {
      throw new Exception("Not valid query")
    }
  }

  def findEntities(protos: List[EntityProto]): List[Entity] = {
    protos.map { findEntity(_) }.flatten
  }

  // Main entry point for requests in the form of protos
  def fullQuery(proto: EntityProto): List[EntityProto] = {
    if (proto.hasUid && proto.getUid == "*") {
      allQuery
    } else {
      protoTreeQuery(proto).map(_.toProto)
    }
  }

  def getChildren(rootId: Long, relation: String) = {
    from(entities)(ent =>
      where(ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId)))
        select (ent))
  }

  def getParents(rootId: Long, relation: String) = {
    from(entities)(ent =>
      where(ent.id in
        from(edges)(edge =>
          where((edge.childId === rootId) and (edge.relationship === relation))
            select (edge.parentId)))
        select (ent))
  }

  def getParentsWithDistance(rootId: Long, relation: String) = {
    from(entities, edges)((ent, edge) =>
      where(ent.id === edge.parentId and edge.childId === rootId and edge.relationship === relation)
        select (ent, edge.distance))
  }

  def getChildrenWithDistance(rootId: Long, relation: String) = {
    from(entities, edges)((ent, edge) =>
      where(ent.id === edge.childId and edge.parentId === rootId and edge.relationship === relation)
        select (ent, edge.distance))
  }

  // Helper for 
  def getChildrenOfType(rootId: Long, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent))
  }

  def getParentOfType(rootId: Long, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.childId === rootId) and (edge.relationship === relation))
            select (edge.parentId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent))
  }

  def addEntity(name: String, types: String*) = {
    val ent = entities.insert(new Entity(name))
    types.foreach(t => entityTypes.insert(new EntityToTypeJoins(ent.id, t)))
    ent
  }

  def findEdge(parent: Entity, child: Entity, relation: String): Option[Edge] = {
    val matching = edges.where(r => r.parentId === parent.id and r.childId === child.id and r.relationship === relation and r.distance === 1).toList
    if (matching.size == 1) Some(matching.head) else None
  }

  def addEdge(parent: Entity, child: Entity, relation: String) = {
    val originalEdge = edges.insert(new Edge(parent.id, child.id, relation, 1))
    getParentsWithDistance(parent.id, relation).foreach { case (ent, dist) => addDerivedEdge(ent, child, relation, dist + 1, originalEdge) }
    getChildrenWithDistance(child.id, relation).foreach { case (ent, dist) => addDerivedEdge(parent, ent, relation, dist + 1, originalEdge) }
    originalEdge
  }

  def deleteEdge(edge: Edge) = {
    val derivedEdges = deriveds.where(t => t.parentEdgeId === edge.id).toList
    derivedEdges.foreach { de =>
      edges.delete(de.edgeId)
      deriveds.delete(de.id)
    }
    edges.delete(edge.id)
  }

  private def addDerivedEdge(parent: Entity, child: Entity, relation: String, depth: Int, sourceEdge: Edge) = {
    assert(depth > 1)
    val derivedEdge = edges.insert(new Edge(parent.id, child.id, relation, depth))
    deriveds.insert(new Derived(sourceEdge.id, derivedEdge.id))
    derivedEdge
  }

  def addTypeToEntity(ent: Entity, typ: String) = {
    entityTypes.insert(new EntityToTypeJoins(ent.id, typ))
    entities.lookup(ent.id).get
  }

  def findOrCreateEntity(name: String, typ: String): Entity = {
    val list = nameTypeQuery(Some(name), None)
    if (list.size > 1) throw new Exception("more than one entity matched: " + name + " type:" + typ)
    if (list.size == 1) {
      if (list.head.types.value.find(_ == typ).isEmpty) {
        addTypeToEntity(list.head, typ)
      } else {
        list.head
      }
    } else {
      addEntity(name, typ)
    }
  }

  def findEntitiesByName(names: List[String]) = {
    from(entities)(ent => where(ent.name in names) select (ent))
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

  def populateRelations(builder: EntityProto.Builder, entity: Entity, relation: String, descendant: Boolean) = {
    val rels = if (descendant) getChildren(entity.id, relation) else getParents(entity.id, relation)
    val relProto = Relationship.newBuilder.setRelationship(relation).setDescendantOf(descendant)

    rels.toList.foreach(r => relProto.addEntities(entityToProto(r)))
    builder.addRelations(relProto)
    builder
  }

  def entityIdsFromType(typ: String) = {
    from(entityTypes)(t =>
      where(t.entType === typ)
        select (&(t.entityId)))
  }

  def findIdsOfChildren(rootNode: EntityProto, relation: String, childType: String): Query[Long] = {
    if (rootNode.uid == Some("*") || rootNode.name == Some("*")) {
      entityIdsFromType(childType)
    } else {
      val rootEnt = EntitySearches.findRecord(rootNode).get
      from(getChildrenOfType(rootEnt.id, relation, childType))(ent => select(&(ent.id)))
    }
  }
}

trait EntitySearches extends UniqueAndSearchQueryable[EntityProto, Entity] {
  val table = ApplicationSchema.entities
  def uniqueQuery(proto: EntityProto, sql: Entity) = {
    List(
      proto.uid.asParam(sql.id === _.toLong),
      proto.name.asParam(sql.name === _),
      EQ.noneIfEmpty(proto.types).asParam(sql.id in EQ.entityIdsFromTypes(_)))
  }

  def searchQuery(proto: EntityProto, sql: Entity) = Nil
}
object EntitySearches extends EntitySearches

object EQ extends EntityQueries
