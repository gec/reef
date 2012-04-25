package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.proto.StatusCodes
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.exception.ExpectationException


object ScalaResponse {

  class RichResponse[A](resp: Response[A]) {
    import java.util.List

    def many: List[A] = checkGood

    def one: A = {
      inspect {
        case (list, 0) => throw new ExpectationException("Expected a response list of size 1, but got an empty list")
        case (list, 1) => list.get(0)
        case (list, count) => throw new ExpectationException("Expected a response list of size 1, but got a list of size: " + count)
      }
    }
    def oneOrNone: Option[A] = {
      inspect {
        case (list, 0) => None
        case (list, 1) => Some(list.get(0))
        case (list, count) => throw new ExpectationException("Expected a response list of size 1, but got a list of size: " + count)
      }
    }

    private def inspect[B](f: (List[A], Int) => B): B = {
      val list = checkGood
      f(list, list.size())
    }

    private def checkGood: List[A] = {
      resp.isSuccess match {
        case true => resp.getList
        case false => throw StatusCodes.toException(resp.getStatus, resp.getError)
      }
    }
  }

  implicit def _scalaResponse[A](resp: Response[A]): RichResponse[A] = new RichResponse(resp)
}

