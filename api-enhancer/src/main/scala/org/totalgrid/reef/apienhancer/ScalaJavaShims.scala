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
 *
 * currently the implicts to convert to java objects can't see into the Promises to know to
 * change the type in a map
 */
class ScalaJavaShims(isFuture: Boolean) extends ApiTransformer with GeneratorFunctions {

  val exName = if (isFuture) "AsyncJavaShim" else "JavaShim"
  val japiPackage = if (isFuture) "async." else ""
  val targetEx = if (isFuture) "Async" else ""

  def make(c: ClassDoc, packageStr: String, outputDir: File, sourceFile: File) {
    getFileStream(packageStr, outputDir, sourceFile, ".client.service." + japiPackage + "impl", true, c.name + exName) { (stream, javaPackage) =>
      javaShimClass(c, stream, javaPackage)
    }
  }

  private def javaShimClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName)

    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName()))
    stream.println("import scala.collection.JavaConversions._")
    stream.println("import org.totalgrid.reef.client.sapi.rpc.util.Converters._")
    stream.println("import org.totalgrid.reef.client.service." + japiPackage + "{" + c.name + targetEx + "=> JInterface }")

    if (isFuture) {
      stream.println("import org.totalgrid.reef.client.Promise")
      //stream.println("import org.totalgrid.reef.client.javaimpl.PromiseWrapper")
    }
    stream.println("import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._")

    stream.println("trait " + c.name + exName + " extends JInterface{")

    stream.println("\tdef service: org.totalgrid.reef.client.sapi.rpc." + c.name)

    c.methods.toList.foreach { m =>

      val typAnnotation = typeAnnotation(m, false)

      var msg = "\t" + "override def " + m.name + typAnnotation + "("
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
      implCall += ")"
      implCall += ")"

      if ((m.name.startsWith("find") || m.name.startsWith("clear")) && m.returnType.simpleTypeName != "List") implCall = implCall + ".map(a => convert(a))"
      else if (m.returnType.simpleTypeName() == "SubscriptionResult") implCall = implCall + ".map(a => convert(a))"
      else if (m.returnType.simpleTypeName() == "List") implCall = implCall + ".map(a => seqAsJavaList(a))"
      else if (m.returnType.simpleTypeName() == "Boolean") implCall = implCall + ".map(a => convert(a))"

      if (!isFuture) implCall += ".await"

      /*if (!isFuture)*/ msg += implCall
      /*else msg += "new PromiseWrapper(" + implCall + ")" */

      stream.println(msg)
    }

    stream.println("}")
  }
}