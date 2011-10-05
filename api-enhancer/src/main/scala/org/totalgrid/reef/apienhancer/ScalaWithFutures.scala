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
 * converts the original apis to use scala lists and return Futures
 */
class ScalaWithFutures extends ApiTransformer with GeneratorFunctions {
  def make(c: ClassDoc, packageStr: String, rootDir: File, sourceFile: File) {
    getFileStream(packageStr, rootDir, sourceFile, ".sapi.request", true, c.name) { (stream, javaPackage) =>
      scalaClass(c, stream, javaPackage)
    }
  }

  private def scalaClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")

    // we remove the java list import, so the name List will point to scala.collection.List
    val importMap = Map("java.util.List" -> "")

    c.importedClasses().toList.foreach(p => importMap.get(p.qualifiedTypeName()) match {
      case None => stream.println("import " + p.qualifiedTypeName())
      case _ =>
    })
    stream.println("import org.totalgrid.reef.promise.Promise;")
    stream.println(commentString(c.getRawCommentText()))
    stream.println("trait " + c.name + "{")

    c.methods.toList.foreach { m =>

      var msg = "\t@throws(classOf[ReefServiceException])\n"

      msg += "\t" + "def " + m.name + "("
      msg += m.parameters().toList.map { p =>
        p.name + ": " + scalaTypeString(p.`type`)
      }.mkString(", ")
      msg += ")"
      if (m.returnType.toString != "void")
        msg += ": Promise[" + scalaTypeString(m.returnType) + "]"

      stream.println(commentString(m.getRawCommentText()))
      stream.println(msg)
    }

    stream.println("}")
  }

}