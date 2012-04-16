package org.totalgrid.reef.apienhancer

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

import com.sun.javadoc.ClassDoc
import java.io.{ PrintStream, File }

/**
 *
 * currently the implicts to convert to java objects can't see into the Promises to know to
 * change the type in a map
 */
class ScalaSyncShims extends ApiTransformer with GeneratorFunctions {

  val exName = "SyncShim"

  def make(c: ClassDoc, packageStr: String, outputDir: File, sourceFile: File) {
    getFileStream(packageStr, outputDir, sourceFile, ".client.sapi.sync.impl", true, c.name + exName) { (stream, javaPackage) =>
      javaShimClass(c, stream, javaPackage)
    }
  }

  private def javaShimClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName)

    addScalaImports(stream, c)

    stream.println("import org.totalgrid.reef.client.sapi.sync.{" + c.name + "=> SyncInterface }")

    stream.println("trait " + c.name + exName + " extends SyncInterface{")

    stream.println("\tdef service: org.totalgrid.reef.client.sapi.rpc." + c.name)

    c.methods.toList.foreach { m =>

      val typAnnotation = typeAnnotation(m, false)

      var msg = "\t" + "override def " + m.name + typAnnotation + "("
      msg += m.parameters().toList.map { p =>
        p.name + ": " + javaAsScalaTypeString(p.`type`)
      }.mkString(", ")
      msg += ")"

      msg += " = "

      var implCall = "service." + m.name + "("
      implCall += m.parameters().toList.map { p => p.name }.mkString(", ")
      implCall += ")"

      implCall += ".await"

      msg += implCall

      stream.println(msg)
    }

    stream.println("}")
  }
}