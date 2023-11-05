package model
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

case class Token(id: Int, name: String, symbol: String)

trait TokensTable {

  class Tokens(tag: Tag) extends Table[Token](tag, "tokens") {
    def id = column[Int]("id", O.PrimaryKey)

    def name = column[String]("name", O.Unique)

    def symbol = column[String]("symbol")

    // select
    def * = (id, name, symbol) <> (Token.tupled, Token.unapply)
  }

  val tokens = TableQuery[Tokens]
}
