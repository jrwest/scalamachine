package com.github.jrwest.scalamachine.core

import java.util.Date

trait Resource {
  import Resource._

  //def init: C
  def serviceAvailable(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(true), data)

  def knownMethods(data: ReqRespData): (Res[List[HTTPMethod]],ReqRespData)= (
    default(List(OPTIONS, TRACE, CONNECT, HEAD, GET, POST, PUT, DELETE)),
    data
  )

  def uriTooLong(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(false), data)

  def allowedMethods(data: ReqRespData): (Res[List[HTTPMethod]],ReqRespData) = (default(GET :: Nil), data)

  def isMalformed(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(false), data)

  def isAuthorized(data: ReqRespData): (Res[AuthResult],ReqRespData) = (default(AuthSuccess), data)

  def isForbidden(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(false), data)

  def contentHeadersValid(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(true), data)

  def isKnownContentType(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(true), data)

  def isValidEntityLength(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(true), data)

  def options(data: ReqRespData): (Res[Map[HTTPHeader, String]],ReqRespData) = (default(Map()), data)

  def contentTypesProvided(data: ReqRespData): (Res[ContentTypesProvided],ReqRespData) = {
    (default((ContentType("text/html") -> defaultResponse) :: Nil), data)
  }

  // TODO: change to real content negotiation like ruby port
  def isLanguageAvailable(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(true), data)

  def charsetsProvided(data: ReqRespData): (Res[CharsetsProvided],ReqRespData) = (default(None), data)

  def encodingsProvided(data: ReqRespData): (Res[CharsetsProvided],ReqRespData) = (default(Some(("identity", identity[Array[Byte]](_)) :: Nil)), data)

  def resourceExists(data: ReqRespData): (Res[Boolean],ReqRespData) = (default(true), data)

  def variances(data: ReqRespData): (Res[Seq[String]], ReqRespData) = (default(Nil), data)

  def generateEtag(data: ReqRespData): (Res[Option[String]], ReqRespData) = (default(None), data)

  def lastModified(data: ReqRespData): (Res[Option[Date]], ReqRespData) = (default(None), data)

  def movedPermanently(data: ReqRespData): (Res[Option[String]], ReqRespData) = (default(None), data)

  def previouslyExisted(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  def movedTemporarily(data: ReqRespData): (Res[Option[String]], ReqRespData) = (default(None), data)

  def allowMissingPost(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  def deleteResource(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  def deleteCompleted(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(true), data)

  def postIsCreate(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  def processPost(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  def createPath(data: ReqRespData): (Res[Option[String]], ReqRespData) = (default(None), data)

  def contentTypesAccepted(data: ReqRespData): (Res[ContentTypesAccepted], ReqRespData) = (default(Nil), data)

  def isConflict(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  def expires(data: ReqRespData): (Res[Option[Date]], ReqRespData) = (default(None), data)

  def multipleChoices(data: ReqRespData): (Res[Boolean], ReqRespData) = (default(false), data)

  private def default[A](value: A): Res[A] = ValueRes(value)

  private val defaultResponse: ReqRespData => (Res[Array[Byte]], ReqRespData) = (default("hello, scalamachine".getBytes), _)

}

object Resource {
  type ContentTypesProvided = List[(ContentType, ReqRespData => (Res[Array[Byte]],ReqRespData))]
  type ContentTypesAccepted = List[(ContentType, ReqRespData => (Res[Boolean], ReqRespData))]
  type CharsetsProvided = Option[List[(String,Array[Byte] => Array[Byte])]] // None value specifies charset negotiation short-circuiting
  type EncodingsProvided = Option[List[(String,Array[Byte] => Array[Byte])]] // None values specifies encoding negotiation short-circuiting
}

