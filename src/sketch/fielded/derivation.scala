package sketch.fielded

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror

inline def deriveFielded[T](using m: Mirror.ProductOf[T], nm: NameMapping): Fielded[T] = {
  val labels     = summonFieldNames[m.MirroredElemLabels]
  val extractors = summonExtractors[m.MirroredElemTypes]
  val fields     = labels.zip(extractors).map { case (fieldName, extractor) =>
    extractor.toField(fieldName)
  }
  Fielded(
    fields
  )
}

private inline def summonFieldNames[T <: Tuple]: List[String] = {
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonInline[ValueOf[t]].value.asInstanceOf[String] :: summonFieldNames[ts]
  }
}

private inline def summonExtractors[T <: Tuple]: List[FieldExtractor[?]] = {
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonInline[FieldExtractor[t]] :: summonExtractors[ts]
  }
}

private sealed trait FieldExtractor[T] {
  def toField(fieldName: String): Field[T]
}

private object FieldExtractor {
  given scalar[T](using dt: DataType[T], nm: NameMapping): FieldExtractor[T] with {
    override def toField(fieldName: String): Field[T] = {
      Field.ColumnField(fieldName, Column[T](nm.toColumnName(fieldName), dt))
    }
  }

  given group[T](using fielded: Fielded[T], nm: NameMapping): FieldExtractor[T] with {
    override def toField(fieldName: String): Field[T] = {
      Field.GroupField(fieldName, nm.groupPrefix(fieldName), fielded)
    }
  }
}
