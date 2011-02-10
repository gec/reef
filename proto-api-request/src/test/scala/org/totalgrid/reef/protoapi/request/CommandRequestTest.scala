package org.totalgrid.reef.protoapi.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Commands.{CommandRequest, UserCommandRequest}

object Commands extends CommandRequestBuilders


@RunWith(classOf[JUnitRunner])
class CommandRequestTest extends ServiceClientSuite with ShouldMatchers {

  override def beforeAll() {
    factory.start
    val waiter = new ServiceClientSuite.BrokerConnectionState
    factory.addConnectionListener(waiter)
    waiter.waitUntilStarted()
  }

  override def afterAll() {
    factory.stop
  }


  /*test("Testtest") {
    val req = Commands.allowAccessForCommand("StaticSubstation.Breaker02.Trip")
    val resp = client.putOneOrThrow(req)

    //Documenter.document("Allow access for command", req, resp)

    val req2 = UserCommandRequest.newBuilder.setCommandRequest(CommandRequest.newBuilder.setName("CMD01")).build
    println(Documenter.messageToXml(req))
    println(Documenter.messageToXml(req2))

    val doc = new Documenter("protodoc.xml", "Test Doc")
    doc.addCase("Allow access for command", req, resp)
    doc.save
  }*/
}