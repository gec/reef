package org.totalgrid.reef.protoapi.client

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, RequestEnv }
import ProtoServiceTypes._

import com.google.protobuf.GeneratedMessage

trait ScatterGatherOperations extends AsyncOperations {

  def asyncGetOneScatter[T <: GeneratedMessage](list: List[T])(resp: List[T] => Unit): Unit = asyncVerbScatter[T](list, resp, asyncGetOne[T](_, new RequestEnv))

  private def asyncVerbScatter[T <: GeneratedMessage](list: List[T], resp: List[T] => Unit, async_verb: (T) => ((SingleResult[T]) => Unit) => Unit): Unit = {

    // short circuit so we don't do any unnecessary server queries with a list of length 0
    if (list.size == 0) {
      resp(Nil)
      return
    }
    val map = new java.util.concurrent.ConcurrentHashMap[Int, SingleResult[T]]
    val latch = new java.util.concurrent.CountDownLatch(list.size)

    def gather(idx: Int)(value: SingleResult[T]) {
      map.putIfAbsent(idx, value)
      latch.countDown()
      // last callback will pass the results to user code
      if (latch.getCount == 0) {
        val results = list.indices.map { i =>
          map.get(i).asInstanceOf[SingleResult[T]] match {
            case SingleResponse(x) => x
            case x: Failure => throw x.toException
          }
        }
        // make the final callback to the user code
        resp(results.toList)
      }
    }
    // scatter the requests with the index as the gather id
    list.zipWithIndex.foreach {
      case (proto, i) =>
        // set the callback of the underlying get/put/delete function to our gather function
        async_verb(proto)(gather(i))
    }
  }

  private def throwFailures[T <: GeneratedMessage](result: MultiResult[T]): List[T] = {
    result match {
      case MultiResponse(list) => list
      case x: Failure => throw x.toException
    }
  }

}