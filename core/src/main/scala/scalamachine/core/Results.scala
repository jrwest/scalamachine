package scalamachine.core

import scalamachine.internal.scalaz.{Functor, Monad}

sealed trait Res[+A]
case class ValueRes[+A](value: A) extends Res[A]
case class HaltRes(code: Int, body: Option[HTTPBody] = None) extends Res[Nothing]
case class ErrorRes(errorBody: HTTPBody) extends Res[Nothing]
case object EmptyRes extends Res[Nothing]

trait ResOps[A] {
  def res: Res[A]

  def map[B](f: A => B): Res[B] = {
    res match {
      case ValueRes(r) => ValueRes(f(r))
      case ErrorRes(e) => ErrorRes(e)
      case HaltRes(c,b)  => HaltRes(c,b)
      case EmptyRes => EmptyRes
    }
  }

  def flatMap[B](f: A => Res[B]): Res[B] = res match {
    case ValueRes(v) => f(v)
    case ErrorRes(e) => ErrorRes(e)
    case HaltRes(c,b) => HaltRes(c,b)
    case EmptyRes => EmptyRes
  }

  def orElse[B >: A](other: Res[B]): Res[B] = res match {
    case ValueRes(r) => ValueRes(r)
    case _ => other
  }

  def getOrElse[B >: A](default: => B) = res match {
    case ValueRes(a) => a
    case _ => default
  }

  def filter(p: A => Boolean): Res[A] = res match {
    case ValueRes(a) => if (p(a)) ValueRes(a) else EmptyRes
    case ErrorRes(e) => ErrorRes(e)
    case HaltRes(c,b) => HaltRes(c,b)
    case EmptyRes => EmptyRes
  }

  def |(default: => A) = getOrElse(default)

  def toOption: Option[A] = res match {
    case ValueRes(a) => Some(a)
    case _ => None
  }
}


object Res extends ResFunctions with ResInternalInstances {

  implicit def resOps[T](r: Res[T]): ResOps[T] = new ResOps[T] {
    def res: Res[T] = r
  }

}

trait ResFunctions {
  def result[A](a: A): Res[A] = ValueRes(a)
  def halt[A](code: Int): Res[A] = HaltRes(code)
  def halt[A](code: Int, body: HTTPBody): Res[A] = HaltRes(code, Option(body))
  def halt[A](code: Int, err: Throwable): Res[A] = HaltRes(code, Option(err.getMessage))
  def error[A](body: HTTPBody): Res[A] = ErrorRes(body)
  def error[A](e: Throwable): Res[A] = error(e.getMessage)
  def empty[A]: Res[A] = EmptyRes
}

trait ResInternalInstances {
  import scalamachine.internal.scalaz.{Monad, Traverse, Applicative}
  implicit val resScalazInstances = new Traverse[Res] with Monad[Res] {
    def point[A](a: => A): Res[A] = ValueRes(a)
    def traverseImpl[G[_],A,B](fa: Res[A])(f: A => G[B])(implicit G: Applicative[G]): G[Res[B]] =
      map(fa)(a => G.map(f(a))(ValueRes(_): Res[B])) match {
        case ValueRes(r) => r
        case HaltRes(c, b) => G.point(HaltRes(c,b))
        case ErrorRes(e) => G.point(ErrorRes(e))
        case _ => G.point(EmptyRes)
      }
    def bind[A, B](fa: Res[A])(f: A => Res[B]): Res[B] = fa flatMap f
  }
}

case class ResTransformer[M[_],A](run: M[Res[A]]) {
  self =>

  def map[B](f: A => B)(implicit F: Functor[M]): ResTransformer[M,B] = {
    ResTransformer(F.map(self.run)((_: Res[A]) map f))
  }

  def flatMap[B](f: A => ResTransformer[M,B])(implicit M: Monad[M]) = {
    ResTransformer(M.bind(self.run) {
      case ValueRes(v) => f(v).run
      case r @ HaltRes(_,_) => M.point(r: Res[B])
      case r @ ErrorRes(_) => M.point(r: Res[B])
      case r @ EmptyRes => M.point(r: Res[B])
    })
  }

  def filter(p: A => Boolean)(implicit M: Monad[M]) = {
    ResTransformer(M.bind(self.run) { res => M.point(res filter p) })
  }
}


object ResTransformer extends ResTransformerFunctions with ResTransformerInstances

trait ResTransformerFunctions {
  import scalamachine.internal.scalaz.~>
  def resT[M[_]] = new (({type λ[α] = M[Res[α]]})#λ ~> ({type λ[α] = ResTransformer[M, α]})#λ) {
    def apply[A](a: M[Res[A]]) = ResTransformer[M, A](a)
  }
}

trait ResTransformerInstances {
  import scalamachine.internal.scalaz.{MonadTrans, Pointed}
  import scalamachine.internal.scalaz.syntax.pointed._

  implicit def RTInstances[M[_] : Monad] = new Monad[({type R[X]=ResTransformer[M,X]})#R] {
    def point[A](a: => A): ResTransformer[M,A] = ResTransformer[M,A](Pointed[M].point(Pointed[Res].point(a)))
    def bind[A,B](fa: ResTransformer[M,A])(f: A => ResTransformer[M,B]): ResTransformer[M,B] = fa flatMap f
  }

  implicit val RTMonadTrans = new MonadTrans[ResTransformer] {
    def liftM[G[_], A](ga: G[A])(implicit G: Monad[G]): ResTransformer[G,A] =
      ResTransformer[G,A](G.map(ga)(_.point[Res]))

    implicit def apply[G[_]: Monad]: Monad[({type R[X]=ResTransformer[G,X]})#R] = RTInstances[G]
  }
}

sealed trait AuthResult {
  def fold[T](success: T, failure: String => T): T = this match {
    case AuthSuccess => success
    case AuthFailure(s) => failure(s)
  }
}
case object AuthSuccess extends AuthResult
case class AuthFailure(headerValue: String) extends AuthResult
