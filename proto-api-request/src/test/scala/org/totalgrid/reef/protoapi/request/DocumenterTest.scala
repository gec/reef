package org.totalgrid.reef.protoapi.request


import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.scalatest.FunSuite
import org.totalgrid.reef.proto.Commands.{CommandStatus, CommandRequest, UserCommandRequest}


@RunWith(classOf[JUnitRunner])
class DocumenterTest extends FunSuite with ShouldMatchers {



  test ("Doctest") {
    //val req = Commands.allowAccessForCommand("StaticSubstation.Breaker02.Trip")

    val req = UserCommandRequest.newBuilder
      .setCommandRequest(
        CommandRequest.newBuilder.setName("StaticSubstation.Breaker02.Trip"))
      .build

    val resp = UserCommandRequest.newBuilder
      .setUid("502")
      .setStatus(CommandStatus.EXECUTING)
      .setUser("core")
      .setCommandRequest(CommandRequest.newBuilder.setName("StaticSubstation.Breaker02.Trip"))
      .setTimeoutMs(502394324)
      .build


    println(Documenter.messageToXml(req))
    println(Documenter.messageToXml(resp))

    val doc = new Documenter("doctest.xml", "Test Doc", "DOCUMENTATION PARAGRAPH")
    doc.addCase("User command request", "Blah blah blah blah", req, resp)
    doc.addCase("Second thing that happens", "Blah blah blah blah", req, resp)
    doc.save
  }
}