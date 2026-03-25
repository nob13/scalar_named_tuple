package sketch.fielded

import sketch.fielded.{Fielded, Table}

case class Address(
    street: String,
    zip: String,
    city: String
) derives Fielded

case class User(
    id: Int,
    email: String,
    age: Int,
    name: Option[String] = None,
    address: Option[Address] = None
) derives Table

case class Permission(
    id: Int,
    name: String
) derives Table

case class UserPermission(
    userId: Int,
    permissionId: Int
) derives Table

@main
def derivation(): Unit = {

  println(Fielded[Address])
  println(Table[User])
}
