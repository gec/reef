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

import java.io.{ FileOutputStream, PrintStream, File }
import scala.collection.mutable
import com.sun.javadoc._

class HttpServiceCallBindings extends ApiTransformer with GeneratorFunctions {

  class NotEncodableException(msg: String) extends RuntimeException(msg)

  private val serverBindingSections = mutable.Queue.empty[(String, Seq[String])]

  private var newestSourceFile: Long = 0

  override def finish(rootDir: File) {

    val serverBindingFile = new File(rootDir, "../../../http-bridge/http-bridge/src/main/scala/org/totalgrid/reef/httpbridge/servlets/apiproviders/AllScadaServiceApiCallLibrary.scala")
    // TODO: re-enable newest file checking once we are generating inside generated-sources
    if (!serverBindingFile.exists() || serverBindingFile.lastModified() < newestSourceFile || true) {
      println("Genenerating: " + serverBindingFile.getAbsolutePath)
      val stream = new PrintStream(new FileOutputStream(serverBindingFile))
      writeServerBindingFile(stream)
      stream.close()
    } else {
      println("Skipping: " + serverBindingFile.getAbsolutePath)
    }

  }

  def writeServerBindingFile(stream: PrintStream) {
    stream.println("package org.totalgrid.reef.httpbridge.servlets.apiproviders\n")

    stream.println("import org.totalgrid.reef.httpbridge.servlets.helpers.ApiCallLibrary")
    stream.println("import org.totalgrid.reef.client.sapi.rpc.AllScadaService\n")

    stream.println("/**\n * Auto Generated, do not alter!\n */")
    stream.println("class AllScadaServiceApiCallLibrary extends ApiCallLibrary[AllScadaService] {\n\toverride val serviceClass = classOf[AllScadaService]\n\n")

    // sort by class name to keep results stable
    serverBindingSections.sortWith(_._1 < _._1).foreach(_._2.foreach(stream.println(_)))
    stream.println("}")
  }

  def make(c: ClassDoc, packageStr: String, rootDir: File, sourceFile: File) = {

    if (sourceFile.lastModified > newestSourceFile) newestSourceFile = sourceFile.lastModified

    val serverSideSnippets = mutable.Queue.empty[String]

    serverSideSnippets.enqueue("////////////////////\n// " + c.name() + "\n////////////////////")

    c.methods.toList.foreach { m =>
      try {
        serverSideSnippets.enqueue(buildServerApiBinding(m))
      } catch {
        case nee: NotEncodableException =>
          serverSideSnippets.enqueue("// Can't encode " + m.name + " : " + nee.getMessage)
      }
    }

    serverBindingSections.enqueue((c.name(), serverSideSnippets))
  }

  /**
   * produces api binding or throws exception explaining why we can't bind the function
   */
  def buildServerApiBinding(m: MethodDoc): String = {
    val methodName = m.name
    val (returnSize, resultType) = m match {
      case _ if isReturnOptional(m) => ("optional", m.returnType)
      case _ if isReturnList(m) => ("multi", listPayloadType(m.returnType))
      case _ => ("single", m.returnType)
    }

    if (!isProtobufDerivedClass(resultType)) throw new NotEncodableException("Can't serialize non-protobuf response: " + resultType)

    val argStrings = m.parameters().toList.zipWithIndex.map {
      case (p, i) =>
        ("\tval a" + i + " = " + argumentGetter(p, p.`type`), "a" + i)
    }
    val argDefinitions = argStrings.map { _._1 }.mkString("\n")
    val argCalls = argStrings.map { _._2 }.mkString(", ")

    "%s(\"%s\", classOf[%s], args => {\n%s\n\t(c) => c.%s(%s)\n})"
      .format(returnSize, methodName, resultType, argDefinitions, methodName, argCalls)
  }

  /**
   *  we can only handle protobuf return types
   */
  def isProtobufDerivedClass(ptype: Type) = {
    try {
      if (ptype.asClassDoc().superclass().qualifiedTypeName() == "com.google.protobuf.GeneratedMessage") {
        true
      } else {
        false
      }
    } catch {
      case e: Exception => false
    }
  }

  def listPayloadType(ptype: Type) = {
    val argumentTypes = ptype.asParameterizedType().typeArguments().toList
    if (argumentTypes.size != 1) throw new NotEncodableException("Can't parse list with unhandled types " + ptype + " argumentTypes: " + argumentTypes)
    argumentTypes(0)
  }

  def argumentGetter(parameter: Parameter, ptype: Type) = {

    val specialCasedTypes = Map(
      "int" -> "Int",
      "boolean" -> "Boolean",
      "long" -> "Long",
      "String" -> "String",
      "double" -> "Double",
      // "byte" -> "ByteArray",
      "ReefID" -> "Id",
      "ReefUUID" -> "Uuid")

    val (argumentType, pluralizer) = if (ptype.asParameterizedType() != null) {
      (listPayloadType(ptype), "s")
    } else (ptype, "")

    val simpleType = argumentType.simpleTypeName()
    specialCasedTypes.get(simpleType) match {
      case Some(typ) => "args.get" + typ + pluralizer + "(\"" + parameter.name + "\")"
      case None if simpleType == "T" => throw new NotEncodableException("Can't handle paremeterized types")
      case None =>
        throw new NotEncodableException("Can't encode type: " + argumentType)

      //"args.getArgument" + pluralizer + "(\"" + parameter.name + "\", classOf[" + argumentType + "])"
    }
  }
}
