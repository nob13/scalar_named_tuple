package sketch.queries

import sketch.fielded.{Fielded, Optionalize, Rep, Table, User, UserPermission, Permission}

trait QueryBuilder[T] {

  /** Generated SQL. */
  def sql: String

  /** Run the actual query. */
  def run(): Seq[T] = {
    println(s"Running query: ${sql}")
    // Would parse the results from ResultSet now
    Nil
  }

  /** Add a filter */
  def filter(predicate: ColumnPath[?, T] => Rep[Boolean]): QueryBuilder[T] =
    GenericFilter(this, filters = Seq(predicate(path)))

  /** Current path to start on */
  def path: ColumnPath[?, T]

  /** Project elements */
  def map[U](f: ColumnPath[T, T] => ColumnPath[T, U])(using fielded: Fielded[T]): QueryBuilder[U] =
    project(f(ColumnPath.make[T]))

  /** Project with path. */
  def project[U](p: ColumnPath[T, U]): QueryBuilder[U] = Projection[T, U](this, p)

  /** Inner Join */
  def join[U](
      right: QueryBuilder[U]
  )(on: (ColumnPath[?, T], ColumnPath[?, U]) => Rep[Boolean]): QueryBuilder[(T, U)] = {
    Join(this, right, on(path, right.path))
  }

  /** Left Join two Queries */
  def leftJoin[R](right: QueryBuilder[R])(
      on: (ColumnPath[?, T], ColumnPath[?, R]) => Rep[Boolean]
  ): QueryBuilder[(T, Optionalize[R])] = {
    LeftJoin(this, right, on(path, right.path))
  }
}

object QueryBuilder {
  def apply[T](using t: Table[T]): QueryBuilder[T] = TableSelect(t)
}

case class TableSelect[T](table: Table[T], filters: Seq[Rep[Boolean]] = Nil) extends QueryBuilder[T] {
  override def path: ColumnPath[T, T] = ColumnPath.Root(table.fielded)

  override def filter(predicate: ColumnPath[?, T] => Rep[Boolean]): QueryBuilder[T] =
    copy(
      filters = filters :+ predicate(path)
    )

  override def sql: String = {
    val colNames    = table.fielded.columns.map(c => c.name).mkString(", ")
    val whereClause =
      if (filters.isEmpty) ""
      else {
        filters
          .map { filter =>
            s"(${filter.toSql})"
          }
          .mkString("WHERE ", " AND ", "")
      }
    s"SELECT ${colNames} FROM ${table.tableName} ${whereClause}"
  }
}

case class Projection[U, T](underlying: QueryBuilder[U], projection: ColumnPath[U, T]) extends QueryBuilder[T] {
  val applied: ColumnPath[?, T] = underlying.path.append(projection)

  override def sql: String = s"SELECT ${applied.toSql} FROM (${underlying.sql})"

  override def path: ColumnPath[?, T] = projection

  override def project[P](p: ColumnPath[T, P]): QueryBuilder[P] = copy(
    projection = projection.append(p)
  )
}

case class GenericFilter[R](underlying: QueryBuilder[R], filters: Seq[Rep[Boolean]]) extends QueryBuilder[R] {
  override def sql: String = {
    if (filters.isEmpty) {
      underlying.sql
    } else {
      val filterSql = filters.map(filter => s"(${filter.toSql})").mkString("WHERE ", " AND ", "")
      s"SELECT * FROM (${underlying.sql}) ${filterSql}"
    }
  }

  override def path: ColumnPath[?, R] = underlying.path

  override def filter(predicate: ColumnPath[?, R] => Rep[Boolean]): QueryBuilder[R] = {
    copy(
      filters = filters :+ predicate(path)
    )
  }
}

@main
def tableSelectExample(): Unit = {
  val ts = QueryBuilder[User]
  ts.run()
}

@main
def filterSelectExample(): Unit = {
  val ts = QueryBuilder[User]
  ts.filter(_.age > 10).run()
  ts.filter(u => u.age > 18 && u.name === "Fred").run()
}

@main
def tableMapExample(): Unit = {
  val ts = QueryBuilder[User]
  ts.filter(_.age > 18).map(x => x.name).run()
  ts.filter(_.age > 18).map(x => (x.name, x.age)).run() // Needs Tuple Support
  ts.filter(_.age > 18).map(x => (x.address.city)).run()
  ts.filter(_.age > 18).map(_.address).map(_.city).run()
  ts.filter(_.age > 18).map(_.address).run()
}

@main
def joinExample(): Unit = {
  val user           = QueryBuilder[User]
  val userPermission = QueryBuilder[UserPermission]

  val joined = user.join(userPermission)((u, p) => u.id === p.userId)
  joined.map(x => (x._1.id, x._2.permissionId)).run()
}

@main
def leftJoinExample(): Unit = {
  val user           = QueryBuilder[User]
  val userPermission = QueryBuilder[UserPermission]

  val joined                                           = user.leftJoin(userPermission)((u, p) => u.id === p.userId)
  val result: QueryBuilder[(Int, String, Option[Int])] = joined.map(x => (x._1.id, x._1.email, x._2.permissionId))
  result.run()
}

@main
def fullJoinExample(): Unit = {
  val user           = QueryBuilder[User]
  val userPermission = QueryBuilder[UserPermission]
  val permission     = QueryBuilder[Permission]

  val userPermissionName = userPermission
    .join(permission)((up, p) => up.permissionId === p.id)
    .map(x => (x._1.userId, x._2.name))

  val userWithPermissionName = user
    .join(userPermissionName)((u, pn) => u.id === pn._1)
    .map(x => (x._1.name, x._2))

  userWithPermissionName.run()
}
