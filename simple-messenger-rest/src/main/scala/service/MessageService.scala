package service
import conf.AppConf
import dao.MessageTable
import model.{Conversation, TextMessage}
import service.UserService.{db, storageDir, users}
import slick.jdbc.SQLiteProfile.api._

import java.io.File
import java.sql.Timestamp
import java.util.Calendar
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global



object MessageService extends MessageTable with AppConf {

  def init(): List[Unit] = {
    // first create .storage directory
    val dir = new File(storageDir)
    if (!dir.exists) {
      dir.mkdir
    }

    // create table schema
    val f1 = db.run(messagesTblQuery.schema.createIfNotExists)
    val f2 = db.run(conversationsTblQuery.schema.createIfNotExists)

    Await.result(Future.sequence(List(f1,f2)), 5.seconds)
  }

  def getMessagesByConversation(conversationId: Long): Seq[(Long,Long,String)] = {
    val f = db.run(messagesTblQuery.filter(_.conversationId === conversationId).result)
    Await.result(f, 5.seconds).map(x => (x._3,x._4,x._5))
  }

  def updateConvo(conversationId: Long, u1 : Long, u2 : Long, msg : String) : Unit = {
    val now = new Timestamp(Calendar.getInstance().getTime().getTime)
    val t1 =
      sqlu"""
              update conversations
              set updated_at = $now
              where conversation_id = $conversationId
              """
    val t2 = messagesTblQuery += (0,conversationId, u1,u2,msg,now,now)
    val combinedAction = DBIO.seq(t1,t2)
    val f = db.run(combinedAction.transactionally)
    Await.result(f, 5.seconds)

  }

  def addConvo(u1: Long, u2: Long, msg: String): Unit = {

    val now = new Timestamp(Calendar.getInstance().getTime().getTime)
    // val insertedConvId = (conversationsTblQuery returning conversationsTblQuery.map(_.conversationId)) += (0L, u1, u2, now, now)
    val t1= conversationsTblQuery +=(0L, u1, u2, now, now)
    db.run(t1).map { idFromDb =>
      val t2 = messagesTblQuery += (0L, idFromDb, u1, u2, msg, now, now)
      val combinedAction = DBIO.seq(t2)
      val f = db.run(combinedAction.transactionally)
      Await.result(f, 5.seconds)
    }


  }

  def getConvoId(u1: Long, u2 : Long): Option[Long] = {
    val f = db.run(conversationsTblQuery.filter(x => x.user1Id === u1 && x.user2Id===u2 ||x.user1Id === u2 && x.user2Id===u1).result.headOption)
    Await.result(f, 5.seconds).map(x => x._1)
  }




}
