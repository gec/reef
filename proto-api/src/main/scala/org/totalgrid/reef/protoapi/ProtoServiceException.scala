package org.totalgrid.reef.protoapi

import org.totalgrid.reef.proto.Envelope

class ProtoServiceException(val msg: String, val status: Envelope.Status = Envelope.Status.BAD_REQUEST) extends RuntimeException(msg) {
  def getStatus = status
  def getMsg = msg
}