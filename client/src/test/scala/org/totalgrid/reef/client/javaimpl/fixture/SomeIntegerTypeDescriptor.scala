package org.totalgrid.reef.client.javaimpl.fixture

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io._
import java.util.List
import org.totalgrid.reef.client.{ServiceProviderInfo, ServicesList}
import org.totalgrid.reef.client.types.{ServiceTypeInformation, ServiceInfo, TypeDescriptor}
import scala.collection.JavaConversions._

case class SomeInteger(num: Int) extends Serializable {
  def increment = SomeInteger(num + 1)
}

object SomeIntegerTypeDescriptor extends SerializableTypeDescriptor[SomeInteger] {
  def id = "SomeInteger"

  def getKlass = classOf[SomeInteger]
}

class SomeIntegerTypeInformation extends ServiceTypeInformation[SomeInteger, SomeInteger] {
  def getDescriptor: TypeDescriptor[SomeInteger] = SomeIntegerTypeDescriptor

  def getSubscriptionDescriptor: TypeDescriptor[SomeInteger] = SomeIntegerTypeDescriptor

  def getEventExchange: String = SomeIntegerTypeDescriptor.id
}


object ExampleServiceList extends ServicesList {
  def info = new ServiceInfo(SomeIntegerTypeDescriptor, SomeIntegerTypeDescriptor)

  def getServiceTypeInformation: List[ServiceTypeInformation[_, _]] = scala.List(new SomeIntegerTypeInformation).toList

  def getServiceProviders: List[ServiceProviderInfo] = Nil
}

trait SerializableTypeDescriptor[A <: Serializable] extends TypeDescriptor[A] {

  final override def serialize(a: A): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bos)
    out.writeObject(a)
    bos.toByteArray()
  }

  final override def deserialize(a: Array[Byte]): A = {
    val bis = new ByteArrayInputStream(a)
    val in = new ObjectInputStream(bis)
    in.readObject().asInstanceOf[A]
  }
}