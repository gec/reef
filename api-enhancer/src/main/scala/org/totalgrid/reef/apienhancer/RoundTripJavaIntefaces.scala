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
 * just duplicates the original interface, proves that we are getting all the information needed to
 * create a working interface without losing information. In fact we may want to move the source of the
 * api-requests somewhere else and only compile the transformed code.
 */
class RoundTripJavaIntefaces extends ApiTransformer with GeneratorFunctions {
  def make(c: ClassDoc, packageStr: String, outputDir: File, sourceFile: File) {

    getFileStream(packageStr, outputDir, sourceFile, ".japi2.request", false, c.name) { (stream, javaPackage) =>
      duplicateJava(c, stream, javaPackage)
    }
  }

  private def duplicateJava(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")
    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName() + ";"))
    stream.println(commentString(c.getRawCommentText()))
    stream.println("public interface " + c.name + "{")

    c.methods.toList.foreach { m =>

      var msg = "\t" + typeString(m.returnType) + " " + m.name + "("
      msg += m.parameters().toList.map { p =>
        typeString(p.`type`) + " " + p.name
      }.mkString(", ")
      msg += ") throws ReefServiceException;"
      stream.println(commentString(m.getRawCommentText()))
      stream.println(msg)
    }
    stream.println("}")
  }
}