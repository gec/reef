package org.totalgrid.reef.client.javaimpl

import org.totalgrid.reef.client.operations.Response
import java.util.List
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.sapi.client.{ Response => SResponse }

object ResponseWrapper {

  def success[A](status: Status, results: List[A]): Response[A] = {
    new ResponseWrapper(status, results, "", true)
  }
  def failure[A](status: Status, error: String): Response[A] = {
    new ResponseWrapper[A](status, null, error, false)
  }

  def convert[A](resp: SResponse[A]): Response[A] = {
    import scala.collection.JavaConversions._
    new ResponseWrapper(resp.status, resp.list, resp.error, resp.success)
  }
}

class ResponseWrapper[A](status: Status, results: List[A], error: String, success: Boolean) extends Response[A] {

  def getStatus: Status = status

  def getList: List[A] = results

  def getError: String = error

  def isSuccess: Boolean = success
}
