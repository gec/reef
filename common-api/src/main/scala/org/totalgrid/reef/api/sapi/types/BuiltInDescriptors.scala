package org.totalgrid.reef.api.sapi.types

import org.totalgrid.reef.api.japi.TypeDescriptor

object BuiltInDescriptors {
  def authRequest() = new TypeDescriptor[org.totalgrid.reef.api.japi.SimpleAuth.AuthRequest] {
    def serialize(typ: org.totalgrid.reef.api.japi.SimpleAuth.AuthRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.api.japi.SimpleAuth.AuthRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.api.japi.SimpleAuth.AuthRequest]
    def id = "auth_request"
  }
  def authRequestServiceInfo = ServiceInfo(authRequest)
}