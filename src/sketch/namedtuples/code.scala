package sketch.namedtuples

@main
def named1(): Unit = {
  // Named tuples have their fields with name

  val named1: (name: String, age: Int) = (name = "Alice", age = 42)
  println(s"Name: ${named1.name}")
}

@main
def selectableDynamic(): Unit = {
  import scala.language.dynamics

  // With dynamic we can have fields with runtime dispatch

  object Selector extends Dynamic {
    def selectDynamic(name: String): Int = name.length
  }

  println(Selector.foo)
}

@main
def named2(): Unit = {

  // We can rip of the names from a case class

  case class Person(
      name: String,
      age: Int
  )

  object Selector extends Selectable {

    type Fields = NamedTuple.Map[NamedTuple.From[Person], Child]

    type Child[_] = Int

    def selectDynamic(name: String): Int = name.length
  }

  println(Selector.name)
}

def helper(): (name: String, age: Int) = {
  (name = "Alice", age = 42)
}

@main
def callHelper(): Unit = {
  val result = helper()
  println(result.name)
}
