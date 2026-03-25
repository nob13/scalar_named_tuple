package sketch.namedtuples

// Drops Option if present
type UnOption[T] = T match
  case Option[x] => x
  case _         => T

class Navigator2[T](path: List[String] = Nil) extends Selectable:
  def show: String = path.reverse.mkString(",")

  type Fields   = NamedTuple.Map[NamedTuple.From[T], Child]
  type Child[C] = Navigator2[UnOption[C]]

  def selectDynamic(name: String): Navigator2[?] =
    Navigator2(name :: path)

@main
def navigator2Example(): Unit = {
  val navigator = Navigator2[User]()
  println(navigator.name.show)
  println(navigator.permission.write.show)
  println(navigator.address.show)
  println(navigator.address.zip.show) // works now, although address is an Option

  // We know types:
  val name: Navigator2[String]   = navigator.name
  val write: Navigator2[Boolean] = navigator.permission.write

  /*
  val address: Navigator2[Address] = navigator.address // This is odd, should be optional
  val addressZip: Navigator2[String] = navigator.address.zip // And this is although wrong, argh!
  
  // We also can't start from Option values (Think left joins)
  val navigatorOpt = Navigator2[Option[User]]()
  // navigatorOpt.permission // Doesn't work, permission is not a field of Option
   */
}
