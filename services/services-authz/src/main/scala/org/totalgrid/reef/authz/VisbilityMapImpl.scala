package org.totalgrid.reef.authz

import java.util.UUID
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast._
import org.totalgrid.reef.models.{SquerylConversions, ApplicationSchema}


class VisiblityMapImpl(permissions : List[Permission]) extends VisibilityMap {
  def selector(resourceId: String)(fun: (Query[UUID]) => LogicalBoolean) = {

    val option = constructQuery(permissions, resourceId, "read")

    if (option.isDefined) {
      fun(option.get)
    } else {
      (false === true)
    }
  }

  private def constructQuery(permissions: List[Permission], service: String, action: String): Option[Query[UUID]] = {
    // first filter down to permissions that have right service+action
    val applicablePermissions = permissions.filter(_.applicable(service, action))

    //println(service + ":" + action + " " + permissions + " -> " + applicablePermissions)

    if (applicablePermissions.isEmpty) {
      None
    } else {
      if (applicablePermissions.find(_.resourceDependent).isEmpty) {
        applicablePermissions.head.allow match {
          case true => Some(from(ApplicationSchema.entities)(sql => select(sql.id)))
          case false => None
        }
      } else {
        applicablePermissions.find(_.selector() != None).map{_ =>
          from(ApplicationSchema.entities)(sql => where(makeSelector(applicablePermissions, sql.id)) select (sql.id))
        }
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
