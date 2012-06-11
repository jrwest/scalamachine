package com.github.jrwest.scalamachine.finagle

import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.twitter.finagle.{Service, SimpleFilter}
import com.github.jrwest.scalamachine.core.dispatch.DispatchTable
import com.twitter.util.Future
import com.github.jrwest.scalamachine.netty.{FixedLengthResponse, NettyHttpResponse}

class FinagleWebmachineFilter(dispatchTable: DispatchTable[HttpRequest,NettyHttpResponse,Future])
  extends SimpleFilter[HttpRequest, HttpResponse] {
  def apply(request: HttpRequest, continue: Service[HttpRequest,HttpResponse]) = {
    if (dispatchTable.isDefinedAt(request)) dispatchTable(request) flatMap {
      case FixedLengthResponse(r) => Future(r)
      case _ => Future.exception(new Exception("scalamachine-finagle does not support streaming responses"))
    }
    else continue(request)
  }
}