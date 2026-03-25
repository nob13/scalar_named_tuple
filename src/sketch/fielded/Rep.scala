package sketch.fielded

import sketch.fielded.{DataType, Rep}

/** A Representation for a value (e.g. a column name) */
trait Rep[T] {
  def toSql: String

  def ===(other: Rep[T]): Rep[Boolean] = {
    val self = this
    new Rep[Boolean] {
      override def toSql: String = s"${self.toSql} = ${other.toSql}"
    }
  }

  def >(other: Rep[T]): Rep[Boolean] = {
    val self = this
    new Rep[Boolean] {
      override def toSql: String = s"${self.toSql} > ${other.toSql}"
    }
  }

  def <(other: Rep[T]): Rep[Boolean] = {
    val self = this
    new Rep[Boolean] {
      override def toSql: String = s"${self.toSql} < ${other.toSql}"
    }
  }

  def &&(using T =:= Boolean)(other: Rep[Boolean]): Rep[Boolean] = {
    val self = this
    new Rep[Boolean] {
      override def toSql: String = s"(${self.toSql}) AND (${other.toSql})"
    }
  }

  def ||(using T =:= Boolean)(other: Rep[Boolean]): Rep[Boolean] = {
    val self = this
    new Rep[Boolean] {
      override def toSql: String = s"(${self.toSql}) OR (${other.toSql})"
    }
  }
}

object Rep {
  class Literal[T](value: T)(using dataType: DataType[T]) extends Rep[T] {
    def toSql: String = dataType.toSql(value)
  }

  case class SomeRep[T](underlying: Rep[T]) extends Rep[Option[T]] {
    override def toSql: String = underlying.toSql
  }

  import scala.language.implicitConversions
  implicit def fromLiteral[T](value: T)(using DataType[T]): Literal[T]    = Literal(value)
  implicit def fromLiteralOpt[T](value: T)(using DataType[T]): SomeRep[T] = SomeRep(Literal(value))
}
