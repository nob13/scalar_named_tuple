package sketch.fielded

/** Our Name Mapping strategy */
trait NameMapping {
  def tableName(caseClassName: String): String

  def toColumnName(fieldName: String): String

  def groupPrefix(fieldName: String): String
}

object NameMapping {
  object SnakeCase extends NameMapping {
    override def tableName(caseClassName: String): String = {
      snakeCase(caseClassName)
    }

    override def toColumnName(fieldName: String): String = {
      snakeCase(fieldName)
    }

    override def groupPrefix(fieldName: String): String = {
      snakeCase(fieldName) + "_"
    }

    private def snakeCase(in: String): String = {
      val builder     = StringBuilder()
      var lastIsUpper = false // scalafix:ok
      var first       = true  // scalafix:ok
      in.foreach { c =>
        if c.isUpper && !lastIsUpper && !first then {
          builder += '_'
        }
        builder += c.toLower
        lastIsUpper = c.isUpper
        first = false
      }
      builder.result()
    }
  }
  given default: NameMapping = SnakeCase
}
