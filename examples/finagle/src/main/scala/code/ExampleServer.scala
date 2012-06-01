package code

import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.http.Http
import java.net.InetSocketAddress
import com.github.jrwest.scalamachine.core.dispatch.Route._
import com.github.jrwest.scalamachine.core.dispatch.StringPart
import com.github.jrwest.scalamachine.finagle.{FinagleWebmachineV3, FinagleWebmachineService}
import com.github.jrwest.scalamachine.core.flow.{FlowLogging, FlowRunner}
import resources.{EchoPostBodyResource, EmptyResource, UnavailableResource}

object ScalamachineExample extends FinagleWebmachineV3 {

  route {
    hostMatching {
      routeToken("jordan") :: routeToken("localhost") :: Nil
    } andPathMatching {
      routeToken("unavailable2") :: Nil
    } serve new UnavailableResource
  }

  route {
    pathMatching {
      routeToken("unavailable") :: Nil
    } serve new UnavailableResource
  }

  route {
    pathMatching {
      routeToken("empty") :: Nil
    } serve  new EmptyResource
  }

  route {
    pathMatching {
      routeToken("echo") :: Nil
    } serve new EchoPostBodyResource
  }

  route {
    hostEndingWith {
      routeToken("localhost") :: Nil
    } serve new EmptyResource
  }


  override val flowRunner = new FlowRunner with FlowLogging
}

object ExampleServer extends App {

  val server =
    ServerBuilder()
      .codec(Http())
      .bindTo(new InetSocketAddress(8080))
      .name("FinagleWebmachine")
      .build(new FinagleWebmachineService(ScalamachineExample))

}