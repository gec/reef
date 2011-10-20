package org.totalgrid.reef.api.sapi.client.rest.impl

import org.totalgrid.reef.api.japi.BadRequestException

object ClassLookup {

  def get[A](value: A) = apply[A](value) match {
    case Some(x) => x
    case None => throw new BadRequestException("Value types are not allowed")
  }

  def apply[A](value: A): Option[Class[A]] = value match {
    case x: AnyRef => Some(x.getClass.asInstanceOf[Class[A]])
    case _ => None
  }

}