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
package org.totalgrid.reef.jmx

import javax.management._

class MetricsMBean(name: ObjectName, container: MetricsContainer) extends DynamicMBean {

  def getName: ObjectName = name

  def getAttribute(attribute: String): AnyRef = {
    container.get(attribute).value
  }

  def getAttributes(attributes: Array[String]): AttributeList = {
    val results = new AttributeList()
    attributes.foreach { name =>
      results.add(new Attribute(name, container.get(name).value))
    }
    results
  }

  def getMBeanInfo: MBeanInfo = {
    val attributes = container.getAll.map(kv => new MBeanAttributeInfo(kv._1, kv._2.getClass.getSimpleName, "", true, false, false))

    new MBeanInfo(this.getClass.getName, "Reef MetricsMBean", attributes.toArray, null, null, null)
  }

  def setAttributes(attributes: AttributeList): AttributeList = {
    throw new UnsupportedOperationException("setAttributes is not implemented")
  }

  def invoke(actionName: String, params: Array[AnyRef], signature: Array[String]): AnyRef = {
    throw new UnsupportedOperationException("invoke is not implemented")
  }

  def setAttribute(attribute: Attribute) {
    throw new UnsupportedOperationException("setAttribute is not implemented")
  }
}
