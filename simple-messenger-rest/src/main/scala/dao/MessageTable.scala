package dao
import conf.AppConf

import java.sql.Timestamp
import slick.lifted.Tag
import slick.jdbc.SQLiteProfile.api._

trait MessageTable {

  this : AppConf =>

  class Conversation(tag: Tag) extends Table[(Long, Long, Long,  Timestamp, Timestamp)](tag, "conversations") {
    def conversationId = column[Long]("conversation_id", O.PrimaryKey, O.AutoInc)

    def user1Id = column[Long]("user1_id")

    def user2Id = column[Long]("user2_id")

    def createdAt = column[Timestamp]("created_at")

    def updatedAt = column[Timestamp]("updated_at")

    def * = (conversationId, user1Id, user2Id, createdAt, updatedAt)
  }

  class TextMessage(tag: Tag) extends Table[(Long, Long, Long, Long, String, Timestamp, Timestamp)](tag, "messages") {
    def messageId = column[Long]("message_id", O.PrimaryKey, O.AutoInc)

    def conversationId = column[Long]("conversation_id")

    def senderId = column[Long]("sender_id")

    def recipientId = column[Long]("recipient_id")

    def messageText = column[String]("message_text")

    def createdAt = column[Timestamp]("created_at")

    def updatedAt = column[Timestamp]("updated_at")

    def * = (messageId, conversationId, senderId, recipientId, messageText, createdAt, updatedAt)
  }

  val messagesTblQuery = TableQuery[TextMessage]
  val conversationsTblQuery = TableQuery[Conversation]

}
