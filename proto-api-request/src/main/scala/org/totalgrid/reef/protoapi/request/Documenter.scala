package org.totalgrid.reef.protoapi.request

import com.google.protobuf.GeneratedMessage

import scala.collection.JavaConversions._
import com.google.protobuf.Descriptors.EnumValueDescriptor
import xml.{XML, NodeSeq}

class Documenter(file: String, title: String) {

  protected var usages = List.empty[NodeSeq]

  def addCase[A <: GeneratedMessage](title: String, request: A, response: A) = {
    usages ::= Documenter.document(title, request, response)
  }

  def save = {
    val content =
      <servicedoc>
        <title>{title}</title>
        {usages.reverse}
      </servicedoc>

    XML.save(file, content, "UTF-8", true)
  }
}

object Documenter {

  def document[A <: GeneratedMessage](title: String, request: A, response: A) = {
    <case>
      <title>{title}</title>
      <request>
        {messageToXml(request)}
      </request>
      <response>
        {messageToXml(response)}
      </response>
    </case>
  }


  /*

  <servicedoc>
    <title></title>
    <case>
      <title></title>
      <request>
        <message..>
      </request>
      <response>
        ...
      </response>
    </case>
  </servicedoc>


  Flint says don't do List

  <message className="CommandAccess">
    <field name="access">ALLOWED</field>
    <field name="commands">
      <list>
        <entry>StaticSubstation.Breaker02.Trip</entry>
        <entry>
          <message ...>
          </message>
        </entry>
      </list>
    </field>
    <field name="entity">
      <message className="Entity">
      ...
      </message>
    </field>
  </message>


   */

  def getContent(obj: Any): Any = obj match {
    case list: java.util.List[_] => listField(list.toList)
    case subMessage: GeneratedMessage => messageToXml(subMessage)
    case enum: EnumValueDescriptor => enum.getName
    case x: Any => x.toString
  }

  def listField(list: List[_]) = {
    val entries = list.map { obj =>
      <entry>{getContent(obj)}</entry>
    }
    <list>{NodeSeq.fromSeq(entries)}</list>
  }

  def messageToXml(msg: GeneratedMessage): NodeSeq = {

    val fields = msg.getAllFields.toList.map {
      case (desc, obj) =>
        println("Desc: " + desc.getName)
        println("Obj: " + obj)
        <field name={desc.getName}>{getContent(obj)}</field>
    }

    <message className={msg.getDescriptorForType.getName}>
      {NodeSeq.fromSeq(fields)}
    </message>
  }
}