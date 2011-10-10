package org.totalgrid.reef.messaging.synchronous

import org.totalgrid.reef.broker.api.BrokerConnection
import org.totalgrid.reef.japi.Envelope.Verb
import net.agileautomata.executor4s.{Future, Executor}
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.client.{Response, Failure}
import org.totalgrid.reef.sapi.{ServiceInfo, ClassLookup, BasicRequestHeaders, ServiceList}

class Connection(lookup: ServiceList, conn: BrokerConnection, executor: Executor, timeoutms: Long) {

  private val correlator = new ResponseCorrelator

  def request[A](verb: Verb, payload: A, headers: BasicRequestHeaders, executor: Executor): Future[Response[A]] = {

    val future = executor.future[Response[A]]

    def send(info: ServiceInfo[A,_]) = {
      //val dest = headers.
      //conn.publish(info.descriptor.id, )
    }

    ClassLookup(payload).flatMap(lookup.getServiceOption) match {
      case Some(info) => send(info)
      case None => future.set(Failure(Envelope.Status.BAD_REQUEST, "No info on type: " + payload))
    }

    future
  }
}