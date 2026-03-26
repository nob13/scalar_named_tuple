package withusql

import usql.{ConnectionProvider, Query}
import usql.dao.{KeyedCrudBase, SqlFielded, SqlTabular}
import usql.profiles.BasicProfile.given

import java.nio.charset.StandardCharsets
import java.sql.{Connection, DriverManager}
import java.util.Properties
import scala.util.Using

val reference = new String(
  getClass
    .getResourceAsStream("/reference.sql")
    .readAllBytes(),
  StandardCharsets.UTF_8
)

// Note: this is rough
val referenceLines: Seq[String] = reference.split("(?<=;)\\s+").toSeq.map(_.trim.stripSuffix(";")).filter(_.nonEmpty)

def withDb[T](f: ConnectionProvider ?=> T) = {
  classOf[org.h2.Driver].toString
  val url = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1"
  Using.resource(DriverManager.getConnection(url, new Properties())) { connection =>
    referenceLines.foreach(line => connection.prepareStatement(line).execute)
    given cp: ConnectionProvider = ConnectionProvider.forConnection(using connection)
    f(using cp)
  }
}

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

object User extends KeyedCrudBase[Int, User] {
  def key: KeyColumnPath =
    cols.id

  lazy val tabular: SqlTabular[User] =
    summon
}

@main
def demo(): Unit = {
  withDb {
    def add(id: Int, name: String, age: Int, city: String): Unit = {
      val user = User(
        id,
        age,
        name,
        address = Some(
          Address(street = "", city = city)
        )
      )
      User.insert(user)
    }
    add(1, "Alice", 23, "Warszawa")
    add(2, "Bob", 23, "Berlin")
    add(3, "Charly", 17, "Hamburg")

    val sameAge: Query[(String, Option[String], String, Option[String])] = User.query
      .join(User.query)((l, r) => (l.age === r.age) && (l.id < r.id))
      .map(x => (x._1.name, x._1.address.city, x._2.name, x._2.address.city))

    println(sameAge.all()) // Vector((Alice,Some(Warszawa),Bob,Some(Berlin)))

    println(
      sameAge.sql
    ) // SELECT u.name AS name,u.address_city AS address_city,user_.name AS name1,user_.address_city AS address_city1 FROM "user" AS u JOIN "user" AS user_ ON (u.age = user_.age) AND (u.id < user_.id)

  }
}
