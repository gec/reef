package org.totalgrid.reef.authz

import java.util.UUID
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast._
import org.totalgrid.reef.models.{SquerylConversions, ApplicationSchema}


class VisibilityMapImpl(permissions : List[Permission]) extends VisibilityMap {
  def selector(resourceId: String)(fun: (Query[UUID]) => LogicalBoolean) = {

    val entityQuery = constructQuery(permissions, resourceId, "read")

    entityQuery match {
      case DenyAll => (false === true)
      case AllowAll => (true === true)
      case Select(q) => fun(q)
    }
  }

  private sealed case class EntityQuery(allowAll : Option[Boolean], query : Option[Query[UUID]])
  private object DenyAll extends EntityQuery(Some(false), None)
  private object AllowAll extends EntityQuery(Some(true), None)
  private case class Select(q : Query[UUID]) extends EntityQuery(None, Some(q))

  private def constructQuery(permissions: List[Permission], service: String, action: String): EntityQuery = {
    // first filter down to permissions that have right service+action
    val applicablePermissions = permissions.filter(_.applicable(service, action))

    //println(service + ":" + action + " " + permissions + " -> " + applicablePermissions)

    if (applicablePermissions.isEmpty) {
      DenyAll
    } else {
      if (applicablePermissions.find(_.resourceDependent).isEmpty) {
        applicablePermissions.head.allow match {
          case true => AllowAll
          case false => DenyAll
        }
      } else {
//        applicablePermissions.find(_.selector() != None) match {
//          case Some(_) => Select(from(ApplicationSchema.entities)(sql => where(makeSelector(applicablePermissions, sql.id)) select (sql.id)))
//          case None => DenyAll
//        }
        Select(from(ApplicationSchema.entities)(sql => where(makeSelector(applicablePermissions, sql.id)) select (sql.id)))
      }
    }
  }

  private def makeSelector(applicablePermissions : List[Permission], uuid: ExpressionNode): LogicalBoolean = {
    SquerylConversions.combineExpressions(applicablePermissions.map { perm =>
      val x: Option[LogicalBoolean] = (perm.selector(), perm.allow) match {
        case (Some(query), true) => Some(new BinaryOperatorNodeLogicalBoolean(uuid, new RightHandSideOfIn(query), "in", true))
        case (Some(query), false) => Some(new BinaryOperatorNodeLogicalBoolean(uuid, new RightHandSideOfIn(query), "not in", true))
        case _ => None
      }
      x
    }.flatten)
  }
}
