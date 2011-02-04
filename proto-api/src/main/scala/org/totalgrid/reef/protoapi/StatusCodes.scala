package org.totalgrid.reef.protoapi

import org.totalgrid.reef.proto.Envelope

object StatusCodes {

  def isSuccess(status: Envelope.Status): Boolean = {
    status match {
      case Envelope.Status.OK => true
      case Envelope.Status.CREATED => true
      case Envelope.Status.UPDATED => true
      case Envelope.Status.DELETED => true
      case Envelope.Status.NOT_MODIFIED => true
      case _ => false
    }
  }

}
