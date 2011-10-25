package org.totalgrid.reef.client

import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.client.sapi.ReefServices
import org.totalgrid.reef.api.japi.settings.AmqpSettings
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.api.sapi.client.rest.Connection
import org.totalgrid.reef.broker.BrokerConnection

class ReefFactory(amqpSettings: AmqpSettings) {
  private val factory = new QpidBrokerConnectionFactory(amqpSettings)
  private val exe = Executors.newScheduledThreadPool()

  private var broker = Option.empty[BrokerConnection]
  private var connection = Option.empty[Connection]

  def connect(): Connection = {
    if (connection.isEmpty) {
      broker = Some(factory.connect)
      connection = Some(ReefServices(broker.get, exe))
    }
    connection.get
  }

  def terminate() {
    broker.foreach { _.disconnect }
    exe.terminate()
  }
}