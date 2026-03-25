package sketch.namedtuples

// We keep UnOption

// Makes something optional if it isn't yet
type Optionalize[T] = T match
  case Option[x] => T
  case _         => Option[T]

class Navigator3[R, T](path: List[String] = Nil) extends Selectable:
  def show: String = path.reverse.mkString(",")

  type Fields   = NamedTuple.Map[NamedTuple.From[UnOption[T]], Child]
  type Child[C] = T match
    case Option[r] => Navigator3[R, Optionalize[C]]
    case _         => Navigator3[R, C]

  def selectDynamic(name: String): Navigator3[R, ?] =
    Navigator3(name :: path)

@main
def navigator3Example(): Unit = {
  val navigator: Navigator3[User, User] = Navigator3()
  println(navigator.name.show)
  println(navigator.permission.write.show)
  println(navigator.address.show)
  println(navigator.address.zip.show)

  // We know types:
  val name: Navigator3[User, String]               = navigator.name
  val write: Navigator3[User, Boolean]             = navigator.permission.write
  val address: Navigator3[User, Option[Address]]   = navigator.address     // It detects Adress is Optional
  val addressZip: Navigator3[User, Option[String]] = navigator.address.zip // It detects address.zip is Optional

  // We also start from Optional types
  val navigatorOpt: Navigator3[Option[User], Option[User]] = Navigator3()
  println(navigatorOpt.permission.write.show)

  val writeOpt: Navigator3[Option[User], Option[Boolean]]     = navigatorOpt.permission.write
  val addressZipOpt: Navigator3[Option[User], Option[String]] = navigatorOpt.address.zip
  println(addressZipOpt.show)
}
