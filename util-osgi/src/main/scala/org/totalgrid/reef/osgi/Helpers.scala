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
package org.totalgrid.reef.osgi

import org.osgi.framework.{ ServiceRegistration, ServiceReference, BundleContext }
import org.osgi.util.tracker.ServiceTracker

object Helpers {

  class RichBundleContext(context: BundleContext) {

    def useService[A <: AnyRef, B](klass: Class[A])(f: A => B): Option[B] = {
      val ref = context.getServiceReference(klass.getName)
      val result = context.getService(ref).asInstanceOf[A] match {
        case null => None
        case obj => Some(f(obj))
      }
      context.ungetService(ref)
      result
    }

    def useServices[A <: AnyRef, B](klass: Class[A])(f: A => B): Seq[B] = {
      useServices(klass, None)(f)
    }
    def useServices[A <: AnyRef, B](klass: Class[A], filter: String)(f: A => B): Seq[B] = {
      useServices(klass, Some(filter))(f)
    }
    private def useServices[A <: AnyRef, B](klass: Class[A], filter: Option[String])(f: A => B): Seq[B] = {
      context.getServiceReferences(klass.getName, filter.getOrElse(null)).flatMap { ref =>
        val result = context.getService(ref).asInstanceOf[A] match {
          case null => None
          case obj => Some(f(obj))
        }
        context.ungetService(ref)
        result
      }
    }

    def useServicesWithProperties[A <: AnyRef, B](klass: Class[A])(f: (A, Map[String, Any]) => B): Seq[B] = {
      useServicesWithProperties(klass, None)(f)
    }
    def useServicesWithProperties[A <: AnyRef, B](klass: Class[A], filter: String)(f: (A, Map[String, Any]) => B): Seq[B] = {
      useServicesWithProperties(klass, Some(filter))(f)
    }
    private def useServicesWithProperties[A <: AnyRef, B](klass: Class[A], filter: Option[String])(f: (A, Map[String, Any]) => B): Seq[B] = {
      context.getServiceReferences(klass.getName, filter.getOrElse(null)).flatMap { ref =>
        val result = context.getService(ref).asInstanceOf[A] match {
          case null => None
          case obj => Some(f(obj, getAllProperties(ref)))
        }
        context.ungetService(ref)
        result
      }
    }

    def createService[A, B >: A](obj: A, klass: Class[B]): ServiceRegistration = {
      context.registerService(klass.getName, obj, null)
    }
    def createService[A, B >: A](obj: A, props: Map[String, Any], klass: Class[B]): ServiceRegistration = {
      val javaProps = new java.util.Hashtable[String, Any]()
      props.foreach { case (key, v) => javaProps.put(key, v) }
      context.registerService(klass.getName, obj, javaProps)
    }

    def watchServices[A <: AnyRef](klass: Class[A])(handler: PartialFunction[ServiceEvent[A], Unit]) {

      val tracker = new ServiceTracker(context, klass.getName, null) {
        override def addingService(reference: ServiceReference): AnyRef = {
          val obj = context.getService(reference).asInstanceOf[A]
          val event = ServiceAdded(obj, getAllProperties(reference))
          if (handler.isDefinedAt(event)) {
            handler(event)
          }
          obj
        }

        override def modifiedService(reference: ServiceReference, service: Any) {
          val obj = context.getService(reference).asInstanceOf[A]
          val event = ServiceModified(obj, getAllProperties(reference))
          if (handler.isDefinedAt(event)) {
            handler(event)
          }
        }

        override def removedService(reference: ServiceReference, service: Any) {
          val obj = context.getService(reference).asInstanceOf[A]
          val event = ServiceRemoved(obj, getAllProperties(reference))
          if (handler.isDefinedAt(event)) {
            handler(event)
          }
          context.ungetService(reference)
        }
      }
      tracker.open()
    }
  }

  sealed abstract class ServiceEvent[A](service: A, properties: Map[String, Any])
  case class ServiceAdded[A](service: A, properties: Map[String, Any]) extends ServiceEvent[A](service, properties)
  case class ServiceModified[A](service: A, properties: Map[String, Any]) extends ServiceEvent[A](service, properties)
  case class ServiceRemoved[A](service: A, properties: Map[String, Any]) extends ServiceEvent[A](service, properties)

  private def getAllProperties(ref: ServiceReference): Map[String, Any] = {
    ref.getPropertyKeys.map(key => (key, ref.getProperty(key))).toMap
  }

  implicit def bundleToRichBundle(context: BundleContext): RichBundleContext = {
    new RichBundleContext(context)
  }
}
