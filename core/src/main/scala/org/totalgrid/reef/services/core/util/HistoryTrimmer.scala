package org.totalgrid.reef.services.core.util

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.services.ProtoServiceCoordinator
import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.measurementstore.MeasurementStore

class HistoryTrimmer(ms: MeasurementStore, period: Long, totalMeasurements: Long) extends ProtoServiceCoordinator with Logging {
  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Reactable) {
    if (!ms.supportsTrim) return
    reactor.repeat(period) {
      val trimmed = ms.trim(totalMeasurements)
      info("Trimmed: " + trimmed + " measurements.")
    }
  }
}