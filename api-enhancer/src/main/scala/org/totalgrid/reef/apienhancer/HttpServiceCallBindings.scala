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
  private val clientBindingSections = mutable.Queue.empty[(String, Seq[String])]

  private var newestSourceFile: Long = 0

  override def finish(rootDir: File) {

    val serverBindingFile = new File(rootDir, "../../../http-bridge/http-bridge/src/main/scala/org/totalgrid/reef/httpbridge/servlets/apiproviders/AllScadaServiceApiCallLibrary.scala")
    val clientBindingFile = new File(rootDir, "../../../http-bridge/js-service-client/src/main/web/reef.client.core-services.js")
    // TODO: re-enable newest file checking once we are generating inside generated-sources
    newestSourceFile = System.currentTimeMillis() + 360000
    writeFileIfNewer(serverBindingFile, newestSourceFile) { writeServerBindingFile(_) }
    writeFileIfNewer(clientBindingFile, newestSourceFile) { writeClientBindingFile(_) }
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

  def writeClientBindingFile(stream: PrintStream) {
    stream.println("(function($) {\n\t$.reefServiceList_core = function(client) {\n\t\tvar calls = {};")

    // sort by class name to keep results stable
    clientBindingSections.sortWith(_._1 < _._1).foreach(_._2.foreach(stream.println(_)))

    stream.println("\t\t$.extend(client, calls);\n\t};\n})(jQuery);")
  }

  def make(c: ClassDoc, packageStr: String, rootDir: File, sourceFile: File) = {

    if (sourceFile.lastModified > newestSourceFile) newestSourceFile = sourceFile.lastModified

    val serverSideSnippets = mutable.Queue.empty[String]
    val clientSideSnippets = mutable.Queue.empty[String]

    val comment = "\t\t////////////////////\n\t\t// " + c.name() + "\n\t\t////////////////////"
    serverSideSnippets.enqueue(comment)
    clientSideSnippets.enqueue(comment)

    c.methods.toList.foreach { m =>
      try {
        serverSideSnippets.enqueue(buildServerApiBinding(m))
        clientSideSnippets.enqueue(buildClientApiBinding(m))
      } catch {
        case nee: NotEncodableException =>
          val errorMsg = "\t\t// Can't encode " + m.name + " : " + nee.getMessage
          serverSideSnippets.enqueue(errorMsg)
          clientSideSnippets.enqueue(errorMsg)
      }
    }

    serverBindingSections.enqueue((c.name(), serverSideSnippets))
    clientBindingSections.enqueue((c.name(), clientSideSnippets))
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

  def buildClientApiBinding(m: MethodDoc): String = {
    val methodName = m.name
    val style = m match {
      case _ if isReturnOptional(m) => "SINGLE"
      case _ if isReturnList(m) => "MULTI"
      case _ => "SINGLE"
    }

    val parameterList = m.parameters().toList

    // we special case the handling of some objects like uuid
    val valueExtractors = parameterList.map { extractValue(_) }.flatten.mkString("");

    val argStrings = parameterList.map { p =>
      ("\t\t\t\t\t%s: %s".format(p.name, p.name), p.name)
    }
    val data = if (!argStrings.isEmpty) "\t\t\t\tdata: {\n" + argStrings.map { _._1 }.mkString(",\n") + "\n\t\t\t\t},\n" else ""
    val args = if (!argStrings.isEmpty) argStrings.map { _._2 }.mkString(", ") else ""

    "\t\tcalls.%s = function(%s) {\n%s\t\t\treturn client.apiRequest({\n\t\t\t\trequest: \"%s\",\n%s\t\t\t\tstyle: \"%s\""
      .format(methodName, args, valueExtractors, methodName, data, style) + "\n\t\t\t});\n\t\t};"
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

  def extractValue(parameter: Parameter): Option[String] = {
    val specialCasedTypes = Map(
      "ReefID" -> "Id",
      "ReefUUID" -> "Uuid")

    specialCasedTypes.get(parameter.`type`.simpleTypeName).map { t =>
      "\t\t\tif(%s.value != undefined) %s = %s.value;\n".format(parameter.name, parameter.name, parameter.name)
    }
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
