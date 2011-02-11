package org.totalgrid.reef.api.java.client

import org.totalgrid.reef.api.ServiceTypes._
import scala.collection.JavaConversions._

class Result[A](result: MultiResult[A]) {

  def isSuccess = result match {
    case MultiSuccess(x) => true
    case _ => false
  }

  def get : java.util.List[A] = result match {
    case MultiSuccess(x) => x
    case x : Failure => throw x.toException
  }

}