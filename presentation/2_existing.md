--- 
subtitle: Existing Solutions / Column Names
---

# Existing Solutions: Column Names

* Just storing column names in constants
  ```scala
  object User {
    val tableName = "user"
    object columns {
        val name = "name"
        val age = "age"
        val addressStreet = "address_street"
        val addressZip = "address_zip"
        [...]
    }    
  }
  ```
* Easy to use in SQL
    ```scala
    s"SELECT ${User.columns.name} FROM ${User.tableName} WHERE ${User.columns.age} >= 18
    ```
  * Cumbersome to maintain
  * No type safety
  * Limited query generator support
* Column names can be extracted using Macros, [Magnum](https://github.com/AugustNagro/magnum) can do this
    
--- 
subtitle: Existing Solutions / Description Language
---

## Description Language

* Modeling the structure with a Custom DSL, like [Slick](https://scala-slick.org/):
  ```scala
  class UsersTable(tag: Tag) extends Table[User](tag, "user") {
    def id = column[Int]("id", O.PrimaryKey)
    def email = column[String]("email")
    def age  = column[Option[Int]]("age")
    def name = column[Option[String]]("name")
    def addressStreet = column[Option[String]]("address_street")
    def addressZip    = column[Option[String]]("address_zip")
    def addressCity   = column[Option[String]]("address_city")

    override def * = (id, email, age, name, (addressStreet, addressZip, addressCity)).shaped <> (
      /* [ .. ] Adapter Code */
    )

    object Users {
        val table = TableQuery[UsersTable]
    }
  }    
  ```
  * Generates very nice queries e.g. <InlineCode code="User.table.filter(_.age >= lit(18))" />
  * Needs a lot of boiler plate per class.

---
subtitle: Existing Solutions / Scala SQL
---

## Scala SQL with Named Tuples

* ```scala
  case class User(
    id: Int,
    email: String,
    age: Int,
    name: Option[String] = None,
    addressStreet: Option[String] = None,
    addressZip: Option[String] = None,
    addressCity: Option[String] = None
  )
  object User extends SimpleTable[User]  

  // Query
  db.run(User.select.filter(_.age >= 18))
  ```
* This is what we want
* But no support for nested case classes
* [Implemented by Jamie Thompson](https://github.com/com-lihaoyi/scalasql/pull/81) after last year's great talk.
