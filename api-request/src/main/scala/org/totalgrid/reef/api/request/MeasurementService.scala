package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.api.{ ReefServiceException, ISubscription }

/**
 * Non-exhaustive API for using the reef Measurement services. This API allows the client code to read current measurement
 * values for many points at a time, read historical values for a single measuremnt at a time or publish measurements in
 * batches. For current and historical value functions you can also pass in an ISubscription object which will receive
 * all future measurement changes for those points. Asking for unknown points/ measurements will result in an exception
 */
trait MeasurementService {

  /**
   * gets the most recent measurement for a point
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementByPoint(point: Point): Measurement

  /**
   * gets the current value for a point (specified by name)
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementByName(name: String): Measurement

  /**
   * gets the most recent measurement for a set of points. If any points are unknown the call will throw a bad request
   * exception.
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementsByNames(names: java.util.List[String]): java.util.List[Measurement]

  /**
   * gets the most recent measurement for a set of points (specified by names). If any points are unknown the
   * call will throw a bad request exception.
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementsByPoints(points: java.util.List[Point]): java.util.List[Measurement]

  /**
   * gets the most recent measurement for a set of points and configure a subscription to receive updates for every
   * measurement change
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementsByPoints(points: java.util.List[Point], subscription: ISubscription): java.util.List[Measurement]
  /**
   * gets the most recent measurement for a set of points and configure a subscription to receive updates for every
   * measurement change
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementsByNames(names: java.util.List[String], subscription: ISubscription): java.util.List[Measurement]

  //  /**  TODO: Implement MeasurementHistory API
  //   * get the history
  //   */
  //  @throws(classOf[ReefServiceException])
  //  def getMeasurementHistory(point : Point) : java.util.List[Measurement]
  //  @throws(classOf[ReefServiceException])
  //  def getMeasurementHistory(name : String) : java.util.List[Measurement]
  //
  //  @throws(classOf[ReefServiceException])
  //  def getMeasurementHistory(point : Point, subscription : ISubscription) : java.util.List[Measurement]
  //  @throws(classOf[ReefServiceException])
  //  def getMeasurementHistory(name : String, subscription : ISubscription) : java.util.List[Measurement]

  /**
   * publish a batch of measurements as if the client was a protocol adapter. Can fail for many reasons and most clients
   * should not use this function. If any point is not publishable the whole group should fail.
   * Preconditions for success:
   *   - the points listed in the measurements all need to exist
   *   - the points must be configured to use an appropriate protocol (benchmark or manual) to maintain message stream
   *   - measurement processors must be available to process the measurement (only question during startup)
   */
  @throws(classOf[ReefServiceException])
  def publishMeasurements(measurements: java.util.List[Measurement])
}
