package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.sapi.rpc.MeasurementService

class MeasurementOutputPublisher(client: Client, pointName: String) extends OutputPublisher {

  def publishMeasurement(m: Measurement.Builder) = {
    val srv = client.getRpcInterface(classOf[MeasurementService])
    srv.publishMeasurements(List(m.setName(pointName).build()))
  }
}
