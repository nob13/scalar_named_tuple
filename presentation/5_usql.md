---
subtitle: usql / Overview
---

# usql

- So far we had a toy implementation, the real one is [usql](https://github.com/reactivecore/usql)
- It started as a small JDBC layer for simplifying SQL calls like Anorm
  ```scala
  sql"INSERT INTO person (id, name) VALUES (${1}, ${"Alice"})".execute()
  val all: Vector[(Int, String)] = sql"SELECT id, name FROM person".query.all[(Int, String)]()
  ```
- Got extended with CRUD Generators: 
  * <InlineCode code="insert(), update(), findAll(), findByKey(), deleteByKey(), etc." />
- No dependencies, no effect system, just wraps JDBC
- Configurable Name Mapping at Compile time with Annotations
- QueryBuilder based upon Named Tuples:
  ```scala
  Person.query
      .join(PersonPermission.query)(_.id === _.personId)
      .join(Permission.query)(_._2.permissionId === _.id)
      .filter(_._2.name === "Write")
      .map(_._1._1.name)
  ```
- Still Beta, queries are working and aliased, could be simpler, Postgres Optimizer is rescuing us.
