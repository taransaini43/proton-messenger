package docs.http.scaladsl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import com.github.blemale.scaffeine.{AsyncLoadingCache, Cache, LoadingCache, Scaffeine}
import com.typesafe.config.ConfigFactory
import service.UserService
import spray.json.DefaultJsonProtocol.{StringJsonFormat, immIterableFormat, mapFormat}

import scala.io.StdIn
import scala.util.Try
import spray.json._
import utils.AppUtils

import scala.concurrent.duration.DurationInt

object ApplicationServer  {


  val cache : LoadingCache[String, List[TextMsg]]=
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

    lazy val serviceHost : String = Try(config.getString("service.host")).getOrElse("localhost")
    lazy val servicePort : Int = Try(config.getInt("service.port")).getOrElse(8080)

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
                  complete(HttpEntity(ContentTypes.`application/json`,"{\"status\":\"success\"}" ))
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
            entity(as[String]) { inputStr =>
              import MyJsonProtocol._
              import spray.json._

              val json = inputStr.parseJson
              val uname = json.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString.replaceAll("\"", "")

              deleteCookie("user-cookie-id") {
                complete(HttpEntity(ContentTypes.`application/json`, "{\"status\":\"success\"}"))
              }
            }
          }
        } ~ pathPrefix("send") {
              pathPrefix("text") {
                path("user") {
                  post {
                    extractRequest {
                      request =>
                        cookie("user-cookie-id"){ // can only send text if logged in
                          cookieValue =>
                            entity(as[String]) { inputStr =>
                              val json = inputStr.parseJson
                              val fromUsername = json.asInstanceOf[JsObject].fields.get("from").getOrElse("").toString.replaceAll("\"", "")
                              val toUsername = json.asInstanceOf[JsObject].fields.get("to").getOrElse("").toString.replaceAll("\"", "")
                              val textToSend = json.asInstanceOf[JsObject].fields.get("text").getOrElse("").toString.replaceAll("\"", "")

                              if(cookieValue.value.split("=")(0).equals(fromUsername)){
                                val existingUnreadTexts = cache.get(toUsername)
                                cache.put(toUsername, existingUnreadTexts :+ TextMsg(fromUsername, textToSend))
                                complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\"}"))
                              }else{
                                complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to send message\"}"))
                              }
                            }
                        }

                    }

                  }
      }}
      } ~ pathPrefix("get") {
        path("unread") {
          get {
            cookie("user-cookie-id") { // can only send text if logged in
              cookieValue =>
            entity(as[String]) { inputStr =>
                val uname = inputStr.parseJson.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString.replaceAll("\"", "")
                if (cookieValue.value.split("=")(0).equals(uname)) {
                  val unreadMessagesFromCache = cache.get(uname)
                  // assumption is that this endpoint is used by a SMS interface which will implement the actual send message functionality
                  val res = unreadMessagesFromCache.groupBy(_.from)
                            .map(y => s"{\"username\":\"${y._1}\", \"texts\": [${y._2.map(t => "\"" + t.txt + "\"").mkString(",")}]}")
                              .mkString(",")
                // TODO : 2. post sending, also persist in db/s3/disk to maintain chat history
                  complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"message\": \"You have message(s)\", \"data\" : [$res\n]}"))

              } else {
                  complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"failure\" , \"error_message\":\"need to be logged in to receive message(s)\"}"))
              }
            }
          }
        }
      }} ~ pathPrefix("get") {
                  path("history") {
                    get {
                      // TODO : fetch history persisted on disk/db/s3 (Read messages) corresponding to the user
                     val textsFromUser ="" // TODO
                      complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"data\":$textsFromUser}"))
                    }
                  }
        }~ pathPrefix("get") {
        path("users") {
          get {
              val userData = UserService.getAllUsers().mkString("[",",","]")
              complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"data\":$userData}"))
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
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}


case class UserInput(username: String, passcode: String)
object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val jsonFormat2: JsonFormat[UserInput] = lazyFormat(jsonFormat(UserInput, "i", "foo"))
}

case class TextMsg (from : String, txt : String)

//def formatMsgResponse (user : String, messages :)

