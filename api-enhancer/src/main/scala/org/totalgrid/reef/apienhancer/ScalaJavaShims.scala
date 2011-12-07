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
package org.totalgrid.reef.apienhancer

import scala.collection.JavaConversions._

import com.sun.javadoc.ClassDoc
import java.io.{ PrintStream, File }

/**
 * TODO: finish java shim for futures
 *
 * currently the implicts to convert to java objects can't see into the Promises to know to
 * change the type in a map
 */
class ScalaJavaShims(isFuture: Boolean) extends ApiTransformer with GeneratorFunctions {

  val exName = if (isFuture) "FuturesJavaShim" else "JavaShim"
  val japiPackage = if (isFuture) "japiF." else ""
  val targetEx = if (isFuture) "Futures" else ""

  def make(c: ClassDoc, packageStr: String, rootDir: File, sourceFile: File) {
    getFileStream(packageStr, rootDir, sourceFile, ".client.service.impl", true, c.name + exName) { (stream, javaPackage) =>
      javaShimClass(c, stream, javaPackage)
    }
  }

  private def javaShimClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName)

    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName()))
    stream.println("import scala.collection.JavaConversions._")
    stream.println("import org.totalgrid.reef.client.sapi.client.rpc.framework.Converters._")
    stream.println("import org.totalgrid.reef.client.service." + japiPackage + "{" + c.name + targetEx + "=> JInterface }")
    stream.println("import org.totalgrid.reef.client.sapi.rpc.AllScadaService")

    if (isFuture) stream.println("import org.totalgrid.reef.client.Promise")

    stream.println("trait " + c.name + exName + " extends JInterface{")

    stream.println("\tdef service: AllScadaService")

    c.methods.toList.foreach { m =>

      var msg = "\t" + "override def " + m.name + "("
      msg += m.parameters().toList.map { p =>
        p.name + ": " + javaAsScalaTypeString(p.`type`)
      }.mkString(", ")
      msg += ")"

      if (isFuture) {
        msg += ": Promise[" + javaAsScalaTypeString(m.returnType) + "]"
      } else {
        msg += ": " + javaAsScalaTypeString(m.returnType)
      }

      msg += " = "

      var implCall = "service." + m.name + "("
      implCall += m.parameters().toList.map { p =>
        if (p.`type`().simpleTypeName == "List") p.name + ".toList"
        else p.name
      }.mkString(", ")
      implCall += ")"
      if (!isFuture) implCall += ".await"

      if ((m.name.startsWith("find") || m.name.startsWith("clear")) && m.returnType.simpleTypeName != "List") implCall = "convert(" + implCall + ")"
      else if (m.returnType.simpleTypeName() == "SubscriptionResult") implCall = "convert(" + implCall + ")"

      msg += implCall

      stream.println(msg)
    }

    stream.println("}")
  }
}