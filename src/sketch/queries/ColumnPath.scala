package sketch.queries

import sketch.fielded.{Column, Field, Fielded, Optionalize, Rep, Structure, UnOption}

import scala.annotation.implicitNotFound

sealed trait ColumnPath[R, T] extends Selectable with Rep[T] {
  final type Child[C] = T match {
    case Option[r] => ColumnPath[R, Optionalize[C]]
    case _         => ColumnPath[R, C]
  }

  type Fields = NamedTuple.Map[NamedTuple.From[UnOption[T]], Child]

  /** The structure of Type T. */
  def structure: Structure[T]

  final def columnNames: Seq[String] = structure.columns.map(_.name)

  final def selectDynamic(name: String): ColumnPath[R, ?] = {
    val childStructure = structure.selectField(name) getOrElse {
      throw new IllegalArgumentException(s"Unknown field, available: ${structure.fieldNames.mkString("[", ",", "]")}")
    }
    ColumnPath.Child(childStructure, this, name)
  }

  override def toSql: String = columnNames.mkString(", ")

  /** Prepend a path. */
  def prepend[R2](path: ColumnPath[R2, R]): ColumnPath[R2, T]

  /** Appends a path. */
  def append[T2](path: ColumnPath[T, T2]): ColumnPath[R, T2] = path.prepend(this)
}

object ColumnPath {
  def make[T](using structure: Structure[T]): ColumnPath[T, T] = ColumnPath.Root(structure)

  case class Root[T](structure: Structure[T]) extends ColumnPath[T, T] {
    override def prepend[R2](path: ColumnPath[R2, T]): ColumnPath[R2, T] = {
      path
    }
  }

  case class Child[R, P, T](structure: Structure[T], parent: ColumnPath[R, P], field: String) extends ColumnPath[R, T] {
    override def prepend[R2](path: ColumnPath[R2, R]): ColumnPath[R2, T] = {
      parent.prepend(path).selectDynamic(field).asInstanceOf[ColumnPath[R2, T]]
    }
  }

  sealed trait TuplePath[R, T <: Tuple] extends ColumnPath[R, T] {
    override def structure: Fielded[T]

    override def prepend[R2](path: ColumnPath[R2, R]): TuplePath[R2, T]
  }

  case class EmptyTuplePath[R]() extends TuplePath[R, EmptyTuple] {
    override def prepend[R2](path: ColumnPath[R2, R]): TuplePath[R2, EmptyTuple] = EmptyTuplePath()

    override def structure: Fielded[EmptyTuple] = Fielded.emptyTuple
  }

  case class RecTuplePath[R, H, T <: Tuple](head: ColumnPath[R, H], tail: TuplePath[R, T])
      extends TuplePath[R, H *: T] {
    override def prepend[R2](path: ColumnPath[R2, R]): TuplePath[R2, H *: T] = RecTuplePath(
      head.prepend(path),
      tail.prepend(path)
    )

    override def structure: Fielded[H *: T] = Fielded.recursiveTuple(using head.structure, tail.structure)
  }

  /** Helper for building ColumnPath from Tuple */
  @implicitNotFound("Could not find BuildFromTuple")
  trait BuildFromTuple[T] {
    type CombinedType <: Tuple

    type Root

    def build(from: T): TuplePath[Root, CombinedType]
  }

  object BuildFromTuple {
    type Aux[T, C <: Tuple, R] = BuildFromTuple[T] {
      type CombinedType = C

      type Root = R
    }

    given empty[R]: BuildFromTuple.Aux[EmptyTuple, EmptyTuple, R] =
      new BuildFromTuple[EmptyTuple] {
        override type CombinedType = EmptyTuple

        override type Root = R

        override def build(from: EmptyTuple): TuplePath[R, EmptyTuple] = EmptyTuplePath[R]()
      }

    given rec[H, T <: Tuple, R, TC <: Tuple](
        using tailBuild: BuildFromTuple.Aux[T, TC, R]
    ): BuildFromTuple.Aux[
      (ColumnPath[R, H] *: T),
      H *: TC,
      R
    ] = new BuildFromTuple[ColumnPath[R, H] *: T] {
      override type CombinedType = H *: TC

      override type Root = R

      override def build(from: (ColumnPath[R, H] *: T)): TuplePath[R, CombinedType] = {
        RecTuplePath(from.head, tailBuild.build(from.tail))
      }
    }
  }

  /** Build a ColumnPath from a tuple of Column Paths. */
  implicit def fromTuple[T](in: T)(using b: BuildFromTuple[T]): ColumnPath[b.Root, b.CombinedType] =
    b.build(in)
}
