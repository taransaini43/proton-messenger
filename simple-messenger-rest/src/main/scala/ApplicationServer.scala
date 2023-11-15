package docs.http.scaladsl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import com.github.blemale.scaffeine.{AsyncLoadingCache, Cache, LoadingCache, Scaffeine}
import com.typesafe.config.ConfigFactory
import service.{MessageService, UserService}
import spray.json.DefaultJsonProtocol.{StringJsonFormat, immIterableFormat, mapFormat}

import scala.io.StdIn
import scala.util.{Failure, Success, Try}
import spray.json._
import utils.AppUtils

import scala.concurrent.duration.DurationInt

object ApplicationServer {


  val cache: LoadingCache[String, List[TextMsg]] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(1.hour)
      .maximumSize(500)
      .build((s: String) => List())

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "messenger-system")
    implicit val executionContext = system.executionContext

    val config = ConfigFactory.load()
    UserService.init()
    MessageService.init()

    lazy val serviceHost: String = Try(config.getString("service.host")).getOrElse("0.0.0.0")
    lazy val servicePort: Int = Try(config.getInt("service.port")).getOrElse(8080)

    val routes = {
      // application DSL routes
      pathPrefix("create") {
        path("user") {
          post {
            entity(as[String]) { inputStr =>
              val json = inputStr.parseJson
              // val userInput = json.convertTo[UserInput]
              // Better way is to parse via case classes - defined UserInput below this class for reference
              val uname = json.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString.replaceAll("\"", "")
              val pcode = json.asInstanceOf[JsObject].fields.get("passcode").getOrElse("").toString.replaceAll("\"", "")

              UserService.getUser(uname) match {
                case Some(u) => // found existing user, can't register
                  complete(HttpEntity(ContentTypes.`application/json`, "{“status”:”failure”, “message”:”User already exists”}"))
                case None => // no matching user
                  UserService.createUser(uname, AppUtils.encodeStr(pcode)) // idealy encrypt
                  complete(HttpEntity(ContentTypes.`application/json`, "{\"status\":\"success\"}"))
              }

            }
          }
        }
      } ~
        path("login") {
          post {
            entity(as[String]) { inputStr =>
              import MyJsonProtocol._
              import spray.json._

              val json = inputStr.parseJson
              val uname = json.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString.replaceAll("\"", "")
              val pcode = json.asInstanceOf[JsObject].fields.get("passcode").getOrElse("").toString.replaceAll("\"", "")

              UserService.getUser(uname) match {
                case Some(u) => // found existing user
                  if (pcode.equals(AppUtils.decodeStr(u.pwd))) {

                    setCookie(HttpCookie("user-cookie-id", value = uname)) {
                      complete(HttpEntity(ContentTypes.`application/json`, "{\"status\":\"success\"}"))
                    }
                  } else
                    complete(HttpEntity(ContentTypes.`application/json`, "{\"status\":\"failure\"}"))
                case None => // no matching user
                  complete(HttpEntity(ContentTypes.`application/json`, "{\"status\":\"failure\"}"))
              }
            }
          }
        } ~
        path("logout") {
          post {
            optionalCookie("user-cookie-id") { // can only send text if logged in
              case Some(cookieValue) =>
                entity(as[String]) { inputStr =>
                  import MyJsonProtocol._
                  import spray.json._
                  val json = inputStr.parseJson
                  val userToLogout = json.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString.replaceAll("\"", "")

                  if (cookieValue.value.split("=")(0).equals(userToLogout)) {
                    deleteCookie("user-cookie-id") {
                      complete(HttpEntity(ContentTypes.`application/json`, "{\"status\":\"success\"}"))
                    }
                  } else {
                    complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to logout\"}"))
                  }
                }
              case None =>
                complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to logout\"}"))

            }
          }
        } ~ pathPrefix("send") {
        pathPrefix("text") {
          path("user") {
            post {
              extractRequest {
                request =>
                  optionalCookie("user-cookie-id") { // can only send text if logged in
                    case Some(cookieValue) =>
                      entity(as[String]) { inputStr =>
                        val json = inputStr.parseJson
                        val fromUsername = json.asInstanceOf[JsObject].fields.get("from").getOrElse("").toString.replaceAll("\"", "")
                        val toUsername = json.asInstanceOf[JsObject].fields.get("to").getOrElse("").toString.replaceAll("\"", "")
                        val textToSend = json.asInstanceOf[JsObject].fields.get("text").getOrElse("").toString.replaceAll("\"", "")

                        if (cookieValue.value.split("=")(0).equals(fromUsername)) {
                          val existingUnreadTexts = cache.get(toUsername)
                          cache.put(toUsername, existingUnreadTexts :+ TextMsg(fromUsername, textToSend,toUsername))
                          complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\"}"))
                        } else {
                          complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to send message\"}"))
                        }
                      }
                    case None =>
                      complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to send message\"}"))

                  }

              }

            }
          }
        }
      } ~ pathPrefix("get") {
        path("unread") {
          get {
            optionalCookie("user-cookie-id") { // can only send text if logged in
              case Some(cookieValue) =>
                entity(as[String]) { inputStr =>
                  val uname = inputStr.parseJson.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString.replaceAll("\"", "")
                  if (cookieValue.value.split("=")(0).equals(uname)) {
                    val unreadMessagesFromCache = cache.get(uname)
                    // assumption is that this endpoint is used by a SMS interface which will implement the actual send message functionality
                    val res = unreadMessagesFromCache.groupBy(_.from)
                      .map(y => s"{\"username\":\"${y._1}\", \"texts\": [${y._2.map(t => "\"" + t.txt + "\"").mkString(",")}]}")
                      .mkString(",")

                    // post succesful sending, also persist in db/s3/disk to maintain chat history
                    unreadMessagesFromCache.foreach(txtMsg => {
                      val u1Id = UserService.getUser(txtMsg.from).get.id
                      val u2Id = UserService.getUser(txtMsg.to).get.id
                      MessageService.getConvoId(u1Id,u2Id) match {
                        case Some(cId) => MessageService.updateConvo(cId,u1Id,u2Id,txtMsg.txt)
                        case None => MessageService.addConvo(u1Id,u2Id,txtMsg.txt)
                      }

                    })

                    complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"message\": \"You have message(s)\", \"data\" : [$res\n]}"))
                  } else {
                    complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to receive message(s)\"}"))
                  }
                }
              case None =>
                complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to receive message(s)\"}"))

            }
          }
        }
      } ~ pathPrefix("get") {
        path("history") {
          get {
            entity(as[String]) { inputStr =>
              val user1 = inputStr.parseJson.asInstanceOf[JsObject].fields.get("friend").getOrElse("").toString.replaceAll("\"", "")
              val user2 = inputStr.parseJson.asInstanceOf[JsObject].fields.get("user").getOrElse("").toString.replaceAll("\"", "")

              // fetch history persisted on disk/db/s3 (Read messages) corresponding to the user
              val u1Id = UserService.getUser(user1).get.id
              val u2Id = UserService.getUser(user2).get.id

              val idNameMap = Map (u1Id -> user1, u2Id -> user2)

              val msgs = MessageService.getConvoId(u1Id,u2Id) match {
                case Some(convId) => MessageService.getMessagesByConversation(convId)
                case None => Seq()
              }
              if(msgs.nonEmpty){
                val formattedMsgTrail = msgs.map(x => s"\"${idNameMap.get(x._2).get}\":\"${x._3}\"").mkString(",")
                complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"data\":[${formattedMsgTrail}]}"))
              }else{
                complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"data\":No history found between $user1 and $user2}"))

              }

            }
          }
        }
      } ~ pathPrefix("get") {
        path("users") {
          get {
            val userData = UserService.getAllUsers().map(x=> "\""+x+"\"").mkString(",")
            complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"data\":[$userData]}"))
          }
        }
      } ~ path("pingtest") { // can be turned into a health check
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Server basic get ping response test</h1>"))
        }
      }
    }

    val bindingFuture = Http().newServerAt(serviceHost, servicePort).bind(routes)

    println(s"Server now online. Please navigate to http://localhost:8080/pingtest\nPress RETURN to stop...")

    // The following line will keep the server running
    // until it's manually terminated or encounters an error.
    bindingFuture
      .flatMap(_ => system.whenTerminated)
      .onComplete {
        case Success(_) => println("Server terminated gracefully.")
        case Failure(ex) => println(s"Server terminated with error: ${ex.getMessage}")
      }
  }
}


case class UserInput(username: String, passcode: String)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val jsonFormat2: JsonFormat[UserInput] = lazyFormat(jsonFormat(UserInput, "i", "foo"))
}

case class TextMsg(from: String, txt: String, to : String)

//def formatMsgResponse (user : String, messages :)

