package service

import conf.AppConf
import dao.UsersTable
import model.User
import slick.jdbc.SQLiteProfile.api._

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object UserService extends UsersTable with AppConf{

  def init(): List[Unit] = {
    // first create .storage directory
    val dir = new File(storageDir)
    if (!dir.exists) {
      dir.mkdir
    }

    // create table schema
    val f1 = db.run(users.schema.createIfNotExists)
    Await.result(Future.sequence(List(f1)), 5.seconds)
  }

  def getUser(username : String) : Option[User] = {
    val f = db.run(users.filter(_.username === username).result.headOption)
    Await.result(f, 5.seconds).map(x => (User(x._1,x._2,x._3)))
  }

  def createTestUser() : Unit = {
    val setup = DBIO.seq(
      users += (123,"tj123","pwd1234")
    )

    val setupFuture = Await.result(db.run(setup), 1.seconds)
  }

  def createUser(uname : String, pwd : String): Unit = {
    val addUserAction = DBIO.seq(
      users += (0, uname, pwd)
    )

   Await.result(db.run(addUserAction), 1.seconds)
    // TODO : Exception Handling in case of db failure
  }

  def getAllUsers () : Seq[String] = {
    val f = db.run(users.result)

    Await.result(f, 10.seconds).map(x => x._2)
    // TODO : add none/empty check as well before rturning - make Option
  }

}
