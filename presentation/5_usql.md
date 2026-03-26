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
- Still Beta, queries are working and aliased, could be better pruned, Postgres Optimizer is rescuing us.


---
subtitle: usql / Example
---

<div class="grid grid-cols-[3fr_3fr] gap-6">
<div>
```scala {none|1-5|1-9|1-14|1-17|1-23}{at:4}
def add(id: Int, name: String, age: Int, city: String): Unit = {
  val user = User(id, age, name, address = 
    Some(Address(street = "", city = city)))
  User.insert(user)
}

add(1, "Alice", 23, "Warszawa")
add(2, "Bob", 23, "Berlin")
add(3, "Charly", 17, "Hamburg")

val sameAge: Query[(String, Option[String], String, Option[String])] = 
  User.query
  .join(User.query)((l, r) => (l.age === r.age) && (l.id < r.id))
  .map(x => (x._1.name, x._1.address.city, x._2.name, x._2.address.city))

println(sameAge.all()) 
// Vector((Alice,Some(Warszawa),Bob,Some(Berlin)))

println(sameAge.sql) 
// SELECT u.name AS name,u.address_city AS address_city,user_.name AS name1, 
// user_.address_city AS address_city1 
// FROM "user" AS u JOIN "user" AS user_ 
// ON (u.age = user_.age) AND (u.id < user_.id)
```
</div>
<div>
```scala {none|1-6|1-11|1-21}
case class User(
    id: Int,
    age: Int,
    name: String,
    address: Option[Address] = None
) derives SqlTabular

case class Address(
    street: String,
    city: String
) derives SqlFielded

object User 
  extends KeyedCrudBase[Int, User] {

  def key: KeyColumnPath =
    cols.id

  lazy val tabular: SqlTabular[User] =
    summon
}
```
</div>
</div>
