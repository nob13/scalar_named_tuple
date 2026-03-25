package sketch.queries

import sketch.fielded.{Rep, Structure, Optionalize}

case class Join[L, R](left: QueryBuilder[L], right: QueryBuilder[R], on: Rep[Boolean]) extends QueryBuilder[(L, R)] {
  override def sql: String = 
    s"(${left.sql}) JOIN (${right.sql}) ON ${on.toSql}"
  

  override def path: ColumnPath[?, (L, R)] = {
    // Note: we have no aliasing
    given leftPath: Structure[L]  = left.path.structure
    given rightPath: Structure[R] = right.path.structure
    ColumnPath.make[(L, R)]
  }
}

case class LeftJoin[L, R](left: QueryBuilder[L], right: QueryBuilder[R], on: Rep[Boolean])
    extends QueryBuilder[(L, Optionalize[R])] {
  override def sql: String = 
    s"(${left.sql}) LEFT JOIN (${right.sql}) ON ${on.toSql}"
  

  override def path: ColumnPath[?, (L, Optionalize[R])] = {
    // Note: we have no aliasing
    given leftPath: Structure[L]  = left.path.structure
    given rightPath: Structure[Optionalize[R]] = right.path.structure.optionalize
    ColumnPath.make[(L, Optionalize[R])]
  }
}
