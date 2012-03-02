package org.totalgrid.reef.calc.lib

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.service.proto.Measurements.{Quality, Measurement}

@RunWith(classOf[JUnitRunner])
class InputBucketTest extends FunSuite with ShouldMatchers {
  import InputBucket._

  def makeTraceMeas(v: Int) = {
    Measurement.newBuilder()
      .setName("test01")
      .setType(Measurement.Type.INT)
      .setIntVal(v)
      .setQuality(Quality.newBuilder)
      .setUnit("u")
      .build
  }

  test("SinceLastBucket") {
    val buck = new SingleLatestBucket("test01")
    buck.hasSufficient should equal(false)

    val first = makeTraceMeas(1)
    buck.onReceived(first)
    buck.getSnapshot should equal(List(first))
    buck.hasSufficient should equal(true)
    
    val second = makeTraceMeas(2)
    buck.onReceived(second)
    buck.getSnapshot should equal(List(second))
    buck.hasSufficient should equal(true)
  }
}
