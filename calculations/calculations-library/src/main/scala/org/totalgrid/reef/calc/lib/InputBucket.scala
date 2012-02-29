package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.service.proto.Calculations.CalculationInput
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.sapi.rpc.MeasurementService
import scala.collection.mutable
import org.totalgrid.reef.client.sapi.client.Subscription
import org.totalgrid.reef.client.service.proto.Calculations.SingleMeasurement.MeasurementStrategy
import org.totalgrid.reef.client.service.proto.Model.Point

/*
  sealed trait MeasRequest
  case class SingleLatest(name: String) extends MeasRequest
  case class MultiSince(from: Long) extends MeasRequest
  case class MultiLimit(count: Int) extends MeasRequest


  def subscribeToMeasurementHistoryByName(pointName: String, limit: Int): Promise[SubscriptionResult[List[Measurement], Measurement]]
  def subscribeToMeasurementHistoryByName(pointName: String, since: Long, limit: Int): Promise[SubscriptionResult[List[Measurement], Measurement]]
 */

class MeasInputManager extends InputManager {

  def init(client: Client, config: List[CalculationInput], trigger: Option[EventedTriggerStrategy]) {
    import InputBucket._

    val srv = client.getRpcInterface(classOf[MeasurementService])
    
    /*config.map(InputBucket.build(_)).map {
      case (InputConfig(pt, variable, req), buck: InputBucket) =>
        req match {
          case SingleLatest => srv.getMeasurementByName(pt).map(buck.onReceived(_))
          case MultiSince(from) => {
            // TODO: meaningful limit, standardize calculating relative time => absolute
            val time = System.currentTimeMillis() + from
            srv.subscribeToMeasurementHistoryByName(pt, time, 100).map { subResult =>
              subResult.

            }   //.map(_.foreach(buck.onReceived(_)))
          }
          case MultiLimit(count) => {

          }
        }
    }*/

  }


  def getSnapshot: Map[String, List[Measurement]] = null

  def hasSufficient: Boolean = false

  def cancel() {}
}

/*class MeasurementInputManager(client: Client, config: List[InputConfig], trigger: Option[EventedTriggerStrategy]) extends InputManager {

  private val srv = client.getRpcInterface(classOf[MeasurementService])

  //private val (subscriptions, buckets


  def getSnapshot: Map[String, List[Measurement]] = null

  def hasSufficient: Boolean = false

  def cancel() {
    subscriptions.foreach(_.cancel())
  }
}*/




/*
// if 'from' or 'to' is set this is a time based range, other wise it is a samples range
message MeasurementRange{
    optional  uint64      from_ms = 1;
    optional  uint64      to_ms   = 2;
    optional  uint32      limit   = 3;
}
message SingleMeasurement{
    enum MeasurementStrategy{
        MOST_RECENT = 1;
        //COORDINATED = 2;
    }
    optional MeasurementStrategy  strategy = 1;
}
message CalculationInput{

    optional org.totalgrid.reef.client.service.proto.Model.Point             point         = 1;
    optional string            variable_name = 2;
    optional MeasurementRange  range         = 3;
    optional SingleMeasurement single        = 4;
}

 */



trait MeasBucket {
  def onReceived(m: Measurement)
}

trait InputBucket extends MeasBucket {
  def variable: String

  def getSnapshot: List[Measurement]

  def hasSufficient: Boolean
}

object InputBucket {

  case class InputConfig(point: String, variable: String, request: MeasRequest)

  sealed trait MeasRequest
  case object SingleLatest extends MeasRequest
  case class MultiSince(from: Long) extends MeasRequest
  case class MultiLimit(count: Int) extends MeasRequest

  def build(calc: CalculationInput): (InputConfig, InputBucket) = {

    val pointName = calc.point.name.getOrElse { throw new Exception("Must have input point name") }

    val variable = calc.variableName.getOrElse { throw new Exception("Must have input variable name") }

    val (request: MeasRequest, bucket: InputBucket) = calc.single.strategy.map {
      case MeasurementStrategy.MOST_RECENT => (SingleLatest, new SingleLatestBucket(variable))
      case x => throw new Exception("Uknown single measurement strategy: " + x)
    } orElse {
      calc.range.limit.map(lim => (MultiLimit(lim), new LimitRangeBucket(variable, lim)))
    } orElse {
      calc.range.fromMs.map(from => (MultiSince(from), new FromRangeBucket(variable, from)))
    } getOrElse {
      throw new Exception("Cannot build input from configuration: " + pointName + " " + variable)
    }

    (InputConfig(pointName, variable, request), bucket)
  }


  class FromRangeBucket(val variable: String, from: Long) extends InputBucket {
    private val queue = new mutable.Queue[Measurement]()

    protected def prune() {
      val horizon = System.currentTimeMillis() + from
      while(queue.head.getTime < horizon) {
        queue.dequeue()
      }
    }
    def onReceived(m: Measurement) = {
      queue.enqueue(m)
      prune()
    }
    def getSnapshot: List[Measurement] = {
      prune()
      queue.toList
    }
    def hasSufficient: Boolean = {
      prune()
      queue.size > 0
    }
  }

  class LimitRangeBucket(val variable: String, limit: Int) extends InputBucket {
    private val queue = new mutable.Queue[Measurement]()

    def onReceived(m: Measurement) {
      queue.enqueue(m)
      while (queue.size > limit) {
        queue.dequeue()
      }
    }

    def getSnapshot: List[Measurement] = queue.toList

    def hasSufficient: Boolean = { queue.size == limit }
  }

  class SingleLatestBucket(val variable: String) extends InputBucket {

    protected var meas: Option[Measurement] = None

    def onReceived(m: Measurement) {
      meas = Some(m)
    }

    def getSnapshot: List[Measurement] = meas.toList

    def hasSufficient: Boolean = meas.isDefined
  }
}


