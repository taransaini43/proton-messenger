package model

import java.sql.Timestamp

case class TextMessage(messageId: Long,
                       conversationId: Long,
                       fromUser : Long,
                       toUser : Long,
                       text : String,
                       createdAt : Timestamp,
                       updatedAt : Timestamp)

case class Conversation(id: Long,
                        user1Id: Long,
                        user2Id : Long,
                        createdAt : Timestamp,
                        updatedAt : Timestamp)
