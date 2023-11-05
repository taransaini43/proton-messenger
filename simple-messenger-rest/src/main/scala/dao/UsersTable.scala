package dao

import conf.AppConf
import model.User
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.SQLiteProfile.api._

trait UsersTable {

  this : AppConf =>

  class Users(tag: Tag) extends Table[(Long, String, String)](tag, "users") {
    def id = column[Long]("SUP_ID", O.PrimaryKey,O.AutoInc)
    def username = column[String]("username",O.Unique)
    def pwd = column[String]("pwd")

    // Every table needs a * projection with the same types as the table's type parameter
    def * = (id, username, pwd)
  }

  val users = TableQuery[Users]
}
