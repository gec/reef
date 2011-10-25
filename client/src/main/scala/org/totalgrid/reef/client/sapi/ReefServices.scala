package org.totalgrid.reef.client.sapi

import org.totalgrid.reef.api.sapi.client.rest.impl.DefaultConnection

import org.totalgrid.reef.broker.BrokerConnection
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.client.rpc.impl.AllScadaServiceJavaShim
import org.totalgrid.reef.api.sapi.client.rest.Connection

object ReefServices {
  def apply(broker: BrokerConnection, exe: Executor) = {
    val conn = new DefaultConnection(broker, exe, 5000)
    prepareConnection(conn)
    conn
  }

  def prepareConnection(conn: Connection) {
    ReefServicesList.getServicesList.foreach(conn.addServiceInfo(_))
    conn.addRpcProvider(AllScadaServiceImpl.serviceInfo)
    conn.addRpcProvider(AllScadaServiceJavaShim.serviceInfo)
  }
}