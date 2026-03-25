package sketch.namedtuples

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

class Navigator[T](path: List[String] = Nil) extends Selectable:
  def show: String = path.reverse.mkString(",")

  type Fields   = NamedTuple.Map[NamedTuple.From[T], Child]
  type Child[C] = Navigator[C]

  def selectDynamic(name: String): Navigator[?] =
    Navigator(name :: path)

@main
def navigatorSample(): Unit = {
  val navigator = Navigator[User]()
  println(navigator.name.show)
  println(navigator.address.show)
  println(navigator.permission.write.show)

  /*
  println(navigator.address.zip.show()) // won't work yet, because adress is optional
   */

  // We know types:
  val name: Navigator[String]             = navigator.name
  val write: Navigator[Boolean]           = navigator.permission.write
  val address: Navigator[Option[Address]] = navigator.address
}
