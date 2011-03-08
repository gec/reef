/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import com.google.protobuf.GeneratedMessage

import scala.collection.JavaConversions._
import com.google.protobuf.Descriptors.EnumValueDescriptor
import java.io.File
import xml.{ Node, XML, NodeSeq }
import org.totalgrid.reef.util.BuildEnv
import org.totalgrid.reef.api.{ ServiceTypes, RequestEnv, Envelope, ReefServiceException }

class Documenter(file: String, title: String, desc: Node) {

  def this(file: String, title: String, desc: String) = {
    this(file, title, <div>{ desc }</div>)
  }

  protected var usages = List.empty[Node]

  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: String, request: A, response: A): Unit = {
    addCase(title, verb, <div>{ desc }</div>, request, List(response))
  }
  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: String, request: A, responses: List[A]): Unit = {
    addCase(title, verb, <div>{ desc }</div>, request, responses)
  }
  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: Node, request: A, response: A): Unit = {
    usages ::= Documenter.document(title, verb, desc, request, List(response))
  }
  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: Node, request: A, responses: List[A]): Unit = {
    usages ::= Documenter.document(title, verb, desc, request, responses)
  }

  def save = Documenter.save(file, usages.reverse, title, desc)
}

object Documenter {

  case class CaseExplanation(title: String, desc: Node)

  case class RequestWithExplanation[A <: AnyRef](explanation: CaseExplanation, verb: Envelope.Verb, request: A, results: ServiceTypes.MultiResult[A])

  def document[A](title: String, verb: String, desc: Node, request: A, responses: List[A]) = {
    <case>
      <title>{ title }</title>
      <desc>{ desc }</desc>
      <request verb={ verb }>
        { messageToXml(request.asInstanceOf[GeneratedMessage]) }
      </request>
      <response>
        { responses.map(r => messageToXml(r.asInstanceOf[GeneratedMessage])) }
      </response>
    </case>
  }

  def getErrorResponse(msg: String, code: Envelope.Status): Node = {
    <response status={ code.toString } error={ msg }/>
  }

  def getResponse[A](status: Envelope.Status, responses: List[A]): Node = {
    <response status={ status.toString }>
      { responses.map(r => messageToXml(r.asInstanceOf[GeneratedMessage])) }
    </response>
  }

  def getExplainedCase[A <: AnyRef](req: RequestWithExplanation[A]): Node = {
    getExplainedCase(req.explanation, req.verb, req.request, req.results)
  }

  def getExplainedCase[A <: AnyRef](explanation: CaseExplanation, verb: Envelope.Verb, request: A, results: ServiceTypes.MultiResult[A]): Node = {
    <case>
      <title>{ explanation.title }</title>
      <desc>{ explanation.desc }</desc>
      <request verb={ verb.toString }>
        { messageToXml(request.asInstanceOf[GeneratedMessage]) }
      </request>
      {
        results match {
          case ServiceTypes.Failure(code, string) =>
            getErrorResponse(string, code)
          case ServiceTypes.MultiSuccess(responses) =>
            // TODO: get response code into Multi/SingleSuccess
            getResponse(Envelope.Status.OK, responses)
        }
      }
    </case>
  }

  def getContent(obj: Any): Any = obj match {
    case list: java.util.List[_] => listField(list.toList)
    case subMessage: GeneratedMessage => messageToXml(subMessage)
    case enum: EnumValueDescriptor => enum.getName
    case x: Any => x.toString
  }

  def listField(list: List[_]) = {
    val entries = list.map { obj =>
      <entry>{ getContent(obj) }</entry>
    }
    NodeSeq.fromSeq(entries)
  }

  def messageToXml(msg: GeneratedMessage): NodeSeq = {

    val fields = msg.getAllFields.toList.map {
      case (desc, obj) =>
        <field name={ desc.getName }>{ getContent(obj) }</field>
    }

    <message className={ msg.getDescriptorForType.getName }>
      { NodeSeq.fromSeq(fields) }
    </message>
  }

  def save(fileName: String, nodes: List[Node], title: String, desc: Node = <div/>, path: String = BuildEnv.configPath + "api-request/target/docxml") = {
    val content =
      <servicedoc>
        <title>{ title }</title>
        <desc>{ desc }</desc>
        { nodes.reverse }
      </servicedoc>

    val dir = new File(path)
    dir.mkdirs

    XML.save(path + "/" + fileName, content, "UTF-8", true)
  }
}