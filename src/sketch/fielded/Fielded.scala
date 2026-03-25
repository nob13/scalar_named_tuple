package sketch.fielded

import scala.compiletime.summonInline
import scala.deriving.Mirror
import scala.util.NotGiven

trait DataType[T] {
  def name: String

  override def toString: String = name

  def optionalize: DataType[Optionalize[T]] = DataType.OptionType(this).asInstanceOf[DataType[Optionalize[T]]]

  /** Converts into SQL Literal (note, in practice we would serialize the value as JDBC Prepared Statement Parameter) */
  def toSql(value: T): String
}

object DataType {
  given string: DataType[String] with {
    def name: String = "String"

    override def toSql(value: String): String = s"'$value'" // Note: no escaping for demonstration purposes
  }

  given int: DataType[Int] with {
    override def name: String = "Int"

    override def toSql(value: Int): String = value.toString
  }

  case class OptionType[T](underlying: DataType[T]) extends DataType[Option[T]] {
    override def name: String = s"Option[${underlying.name}]"

    override def optionalize: DataType[Optionalize[Option[T]]] = this.asInstanceOf[DataType[Option[T]]]

    override def toSql(value: Option[T]): String = {
      value match {
        case None        => "NULL"
        case Some(value) => underlying.toSql(value)
      }
    }
  }

  given optionType[T](using underlying: DataType[T]): OptionType[T] = OptionType(underlying)
}

/** Common base type of Column or Fielded. */
sealed trait Structure[T] {
  def columns: Seq[Column[?]]

  def optionalize: Structure[Optionalize[T]]

  /** Select a field, maps columns accordingly */
  def selectField(name: String): Option[Structure[?]]

  /** Returns available field names. */
  def fieldNames: Seq[String]
}

object Structure {
  given fieldAsStructure[T](using f: Fielded[T]): Structure[T] = f
}

/** A Single column of a given type. */
case class Column[T](name: String, dataType: DataType[T]) extends Structure[T] {
  override def columns: Seq[Column[?]] = Seq(this)

  override def optionalize: Column[Optionalize[T]] = copy(dataType = dataType.optionalize)

  override def selectField(name: String): Option[Structure[?]] = None

  override def fieldNames: Seq[String] = Nil
}

/** Something with fields (a case class). */
case class Fielded[T](fields: Seq[Field[?]]) extends Structure[T] {
  override def toString: String = {
    fields.mkString("[", ",", "]")
  }

  override def columns: Seq[Column[?]] = fields.flatMap(_.columns)

  override def optionalize: Fielded[Optionalize[T]] = Fielded(fields.map(_.optionalize))

  /** Select a field, maps columns accordingly */
  def selectField(name: String): Option[Structure[?]] = {
    fields.find(_.fieldName == name).map {
      case c: Field.ColumnField[?] => c.column
      case c: Field.GroupField[?]  =>
        c.fielded.withColumnPrefix(c.columnPrefix)
    }
  }

  private def withColumnPrefix(prefix: String): Fielded[T] = {
    Fielded(fields.map(_.withColumnPrefix(prefix)))
  }

  override def fieldNames: Seq[String] = fields.map(_.fieldName)
}

object Fielded {
  inline def derived[T: Mirror.ProductOf](using NameMapping): Fielded[T] = deriveFielded[T]

  def apply[T](using f: Fielded[T]): Fielded[T] = f

  given fromTable[T](using t: Table[T]): Fielded[T] = t.fielded

  given optionalized[T](using f: Fielded[T]): Fielded[Optionalize[T]] = f.optionalize

  given asOptional[T](using f: Fielded[T], notOption: NotGiven[T <:< Option[?]]): Fielded[Option[T]] =
    f.optionalize.asInstanceOf[Fielded[Option[T]]]

  given emptyTuple: Fielded[EmptyTuple] = Fielded[EmptyTuple](Seq.empty)

  given recursiveTuple[H, T <: Tuple](using head: Structure[H], tailFielded: Fielded[T]): Fielded[H *: T] = {
    val headField = Field.flat("_1", head)
    buildForTuple(headField, tailFielded)
  }

  /**
   * Supports anonymous tuples directly derived from Tuples like (Int, String).
   *
   * The resulting ColumnPath can be applied to other ColumnPaths and anon fields disappear.
   */
  given recursiveAnonymousTuple[H, T <: Tuple](
      using headDataType: DataType[H],
      tailFielded: Fielded[T]
  ): Fielded[H *: T] = {
    val headField = Field.ColumnField("_1", Column("<anon>", headDataType))
    buildForTuple(headField, tailFielded)
  }

  private def buildForTuple[H, T <: Tuple](headField: Field[H], tailFielded: Fielded[T]): Fielded[H *: T] = {
    val tailGroup = tailFielded.fields.zipWithIndex.map { case (field, idx) =>
      val updatedName = s"_${idx + 2}"
      field match {
        case c: Field.ColumnField[?] => c.copy(fieldName = updatedName)
        case g: Field.GroupField[?]  => g.copy(fieldName = updatedName)
      }
    }
    Fielded[H *: T](headField +: tailGroup)
  }
}

/** A Single field within a case class. */
sealed trait Field[T] {
  def fieldName: String

  def columns: Seq[Column[?]]

  def optionalize: Field[Optionalize[T]]

  def withColumnPrefix(prefix: String): Field[T]
}

object Field {
  def flat[T](name: String, structure: Structure[T]): Field[T] = {
    structure match {
      case c: Column[T]  => ColumnField(name, c)
      case f: Fielded[T] => GroupField(name, "", f)
    }
  }

  case class ColumnField[T](fieldName: String, column: Column[T]) extends Field[T] {
    override def columns: Seq[Column[?]] = List(column)

    override def optionalize: ColumnField[Optionalize[T]] = copy(column = column.optionalize)

    override def withColumnPrefix(prefix: String): Field[T] = copy(
      column = column.copy(
        prefix + column.name
      )
    )
  }

  case class GroupField[T](fieldName: String, columnPrefix: String, fielded: Fielded[T]) extends Field[T] {
    override def columns: Seq[Column[?]] = {
      fielded.columns.map(c => c.copy(name = columnPrefix + c.name))
    }

    override def optionalize: Field[Optionalize[T]] = {
      copy(
        fielded = fielded.optionalize
      )
    }

    override def withColumnPrefix(prefix: String): Field[T] = {
      copy(
        columnPrefix = prefix + columnPrefix
      )
    }
  }
}

/** A Table. */
case class Table[T](tableName: String, fielded: Fielded[T])

object Table {
  inline def derived[T](using mirror: Mirror.ProductOf[T], nm: NameMapping): Table[T] = {
    Table(
      tableName = nm.tableName(summonInline[ValueOf[mirror.MirroredLabel]].value),
      fielded = Fielded.derived[T]
    )
  }

  def apply[T](using f: Table[T]): Table[T] = f
}
