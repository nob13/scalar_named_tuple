---
layout: two-cols-header
subtitle: Named Tuples
---

# Named Tuples to Rescue
::left::

* Named Tuples are an extension of Tuples, e.g. <InlineCode code="(Int, String, Float)" />
* Giving their fields names, instead <InlineCode code="_1, _2, _3" />
* Introduced with Scala 3.7
  
```scala {none|1-4|1-5|1-6|none} {at:1}
def helper(): (name: String, age: Int) = {
  (name = "Alice", age = 42)
}

val result = helper()
println(result.name) // prints `Alice`
```

::right::

* The cool thing: You can borrow them from case classes
* Use them in <InlineCode code="Selectable" /> with <InlineCode code="NamedTuple.From" />
```scala {none|1|1-3|1-5|1-8|1-10|1-13} {at:4}
case class Person(name: String, age: Int)

object Selector extends Selectable {

  type Fields = NamedTuple.Map[NamedTuple.From[Person], Child]

  // Child Type Function
  type Child[_] = Int

  def selectDynamic(name: String): Int = name.length
}

println(Selector.name) // prints `4`
```
  
  * Selector has the Child Type defined (<InlineCode code="Int" />) and it's fields auto complete to the ones of <InlineCode code="Person" />.
  
--- 
subtitle: Named Tuples / Navigation
---

## Navigating Case Classes

* We can use this behavior for three tasks
  * Navigating through a case class
  * Extracting Information (e.g. **Column Name**)
  * Preserving Type Information (**Column Type**)

---
subtitle: Named Tuples / Navigator 1
---

<div class="grid grid-cols-[3fr_2fr] gap-6">
<div>

```scala {1-2|1-4|1-5|1-8|1-11|1-14|1-17|1-22}
class Navigator[T](path: List[String] = Nil) extends Selectable:
  def show: String = path.reverse.mkString(",")

  type Fields   = NamedTuple.Map[NamedTuple.From[T], Child]
  type Child[C] = Navigator[C]

  def selectDynamic(name: String): Navigator[?] = 
    Navigator(name :: path)
  

val navigator = Navigator[User]()
navigator.name.show // name
navigator.address.show // address
navigator.permission.write.show // permission,write

// Oops: zip not member of Navigator[Option[Address]]
navigator.address.zip.show // 💥 Does not compile!

// We know types:
navigator.name : Navigator[String]
navigator.permission.write : Navigator[Boolean]
navigator.address: Navigator[Option[Address]]
```

</div>
<div>

```scala
case class User(
    id: Int,
    name: String,
    permission: Permission,
    address: Option[Address]
)

case class Permission(
    write: Boolean,
    read: Boolean
)

case class Address(
    street: String,
    zip: String,
    city: String
)

```

</div>
</div>



--- 
subtitle: Named Tuples / Match Types
---

## Extend with Match Types

* Problem: Navigating into <InlineCode code="Option[T]" /> doesn't work
* Luckily Scala has [Match Types](https://docs.scala-lang.org/scala3/reference/new-types/match-types.html), which derive a Type based on the Input Value:

  ```scala
  type UnOption[T] = T match {
    case Option[x] => x
    case _         => T
  }  
  ```
* And the <InlineCode code="Child" /> inside the Navigator is such a Match Type, so combining:
  ```scala
  type Fields   = NamedTuple.Map[NamedTuple.From[T], Child]
  type Child[C] = Navigator[UnOption[C]]
  ```  
* Finally we can traverse the whole path

---
subtitle: Named Tuples / Navigator 2
---

<div class="grid grid-cols-[3fr_2fr] gap-6">
<div>

```scala {none|1-4|1-5|1-8|1-11|1-12|1-15|1-17|1-18|1-21}
class Navigator2[T](path: List[String] = Nil) extends Selectable:
  def show: String = path.reverse.mkString(",")

  type Fields   = NamedTuple.Map[NamedTuple.From[T], Child]
  type Child[C] = Navigator2[UnOption[C]]

  def selectDynamic(name: String): Navigator2[?] = 
    Navigator2(name :: path)

val navigator = Navigator2[User]()
navigator.permission.write.show // permission,write
navigator.address.zip.show // ✅: address,zip 

// We know types:
navigator.permission.write: Navigator2[Boolean]

navigator.address: Navigator2[Address] // 💥 Should be Option[Address]
navigator.address.zip: Navigator2[String] // 💥 Should be Option[String]

// Starting from Option
Navigator2[Option[User]]().permission // 💥doesn't compile
```

</div>
<div>

```scala {19-21|all}  {at:1}
case class User(
    id: Int,
    name: String,
    permission: Permission,
    address: Option[Address]
)

case class Permission(
    write: Boolean,
    read: Boolean
)

case class Address(
    street: String,
    zip: String,
    city: String
)

type UnOption[T] = T match
  case Option[x] => x
  case _         => T

```

</div>
</div>

--- 
subtitle: Named Tuples / Optionals
---

## Handling Optional Values

* Argh, the type is not correct, we can't just drop the Option. 
* We need to carry that Information with us.
* Three approaches
  * Split up the Navigator: A regular path, or an optional path
  * Keep the optional type and just <InlineCode code="UnOption" /> in <InlineCode code="NamedTuple.From" />
  * Also keep the **starting point**, making it also possible to navigate from Optional values
* Last one is promising as a full SQL Generator needs the starting point anyway (for decoding Results)
* This looks like a **Lens** (and was inspired by)!

---
subtitle: Named Tuples / Navigator 3
---

<div class="grid grid-cols-[3fr_2fr] gap-6">
<div>

```scala {none|1-2|1-7|1-10|1-14|1-20|1-26}{at: 1}
class Navigator3[R, T](path: List[String] = Nil) extends Selectable:
  def show: String = path.reverse.mkString(",")

  type Fields   = NamedTuple.Map[NamedTuple.From[UnOption[T]], Child]
  type Child[C] = T match
    case Option[r] => Navigator3[R, Optionalize[C]]
    case _         => Navigator3[R, C]

  def selectDynamic(name: String): Navigator3[R, ?] =
    Navigator3(name :: path)

val navigator: Navigator3[User, User] = Navigator3()
navigator.permission.write.show // permission,write 
navigator.address.zip.show // address,zip

// We know types:
navigator.name: Navigator3[User, String] 
navigator.permission.write: Navigator3[User, Boolean] 
navigator.address: Navigator3[User, Option[Address]] ✅
navigator.address.zip: Navigator3[User, Option[String]] ✅

// We also start from Optional types
val navigatorOpt: Navigator3[Option[User], Option[User]] = Navigator3()
navigatorOpt.permission.write.show // ✅ permission,write 
navigatorOpt.permission.write: Navigator3[Option[User], Option[Boolean]] ✅
navigatorOpt.address.zip: Navigator3[Option[User], Option[String]] ✅
```

</div>
<div>

```scala {23-25|all}{at: 1} 
case class User(
    id: Int,
    name: String,
    permission: Permission,
    address: Option[Address]
)

case class Permission(
    write: Boolean,
    read: Boolean
)

case class Address(
    street: String,
    zip: String,
    city: String
)

type UnOption[T] = T match
  case Option[x] => x
  case _         => T

type Optionalize[T] = T match
  case Option[x] => T
  case _         => Option[T]

```

</div>
</div>


--- 
subtitle: Named Tuples / Wrap Up
---

## Named Tuples: Wrap Up


- <InlineCode code="NamedTuple.From" /> let us extract the field names of a case class and give it to our own structure
- Match Types let us change the types at compile time
- We can navigate through (optional) case classes, jumping over Optional fields
- The starting and the resulting type is tracked by the Scala Compiler
- We now know
  - The selected **field name**
  - The **data type**
- This is a good starting point to create queries