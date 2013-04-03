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
 * duplicates the original java apis but wraps all of the return values in Futures
 */
class JavaFutures extends ApiTransformer with GeneratorFunctions {
  def make(c: ClassDoc, packageStr: String, outputDir: File, sourceFile: File) {

    getFileStream(packageStr, outputDir, sourceFile, ".client.service.async", false, c.name + "Async") { (stream, javaPackage) =>
      javaFuture(c, stream, javaPackage)
    }
  }

  private def javaFuture(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")
    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName() + ";"))
    stream.println("import org.totalgrid.reef.client.Promise;")
    stream.println(commentString(c.getRawCommentText()))
    stream.println("public interface " + c.name + "Async" + "{")

    c.methods.toList.foreach { m =>

      val typAnnotation: String = typeAnnotation(m, true)

      var msg = "\t" + typAnnotation + "Promise<" + typeString(m.returnType) + "> " + m.name + "("
      msg += m.parameters().toList.map { p =>
        typeString(p.`type`) + " " + p.name
      }.mkString(", ")
      msg += ");"
      stream.println(commentString(m.getRawCommentText()))
      stream.println(msg)
    }

    stream.println("}")
  }
}