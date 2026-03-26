--- 
layout: two-cols
subtitle: Problem / Schema
---

::left::

# Problem

SQL Schema like:

```sql {all|1|2-5|6-8} {at:1}
CREATE TABLE "user" (
  id INT PRIMARY KEY,
  email TEXT NOT NULL,
  age INT NOT NULL,
  name TEXT,
  address_street TEXT,
  address_zip TEXT,
  address_city TEXT
);
```

::right::

And want to model it using:

```scala {all|1|2-5|6,9-13} {at:1}
case class User(
    id: Int,
    email: String,
    age: Int,
    name: Option[String] = None,
    address: Option[Address] = None
)

case class Address(
    street: String,
    zip: String,
    city: String
)
```

---
subtitle: Problem / Goals
---

## What we want

* We want to model SQL tables using **Case Classes**
* Type safe **field selection, column extraction**
* Generating SQL Queries (Filtering, Projection, Joins)
* Compile time check for bad columns and or types
* Code completion
* Share fields in common case classes (like Address)
  * Why? Customer has 100+ Columns in denormalized Tables with a lot of duplicates
* **Option-Support** (SQL loves nullable fields)
* Low boilerplate

--- 
subtitle: Problem / Scope
---

## Out of scope today

* Running queries: happy to generate them
  * Serializing prepared arguments, deserializing ResultSets.
* Customisation
* Collision handling in SQL Statements or Aliasing
* Escaping
* Warning: type mapping at runtime
  * Expect <InlineCode code="asInstanceOf" />
  * But we are talking to SQL Servers anyway, things can go wrong.
  * Unit test your queries!
* Real Implementation: usql
