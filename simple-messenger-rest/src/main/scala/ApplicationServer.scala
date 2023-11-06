package docs.http.scaladsl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import service.UserService

import scala.io.StdIn
import scala.util.Try
import spray.json._
import utils.AppUtils

object ApplicationServer  {

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
                    entity(as[String]) { inputStr =>
                      // TODO : send data to server - can keep in memory cache or a distributed cache like Redis
                      complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\""))
                    }
                  }
      }}
      } ~ pathPrefix("get") {
        path("unread") {
          get {
            // TODO : 1. fetch data/messages set in memory cache for logged in user
            // TODO : 2. post sending, also persist in db/s3/disk to maintain chat history
            val unreadMessagesFromCache = "" // TODO
            complete(HttpEntity(ContentTypes.`application/json`, s"{\"status\":\"success\" , \"“message”\":$unreadMessagesFromCache}"))
          }
        }
      } ~ pathPrefix("get") {
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
        // test routes - ideally scalatests or junits
      } ~ path("pingtest") { // can be turned into a health check
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Server basic get ping response test</h1>"))
        }
      } ~ path("createTestUser") {
        get {
          UserService.createTestUser()
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Server basic persistence test</h1>"))
        }
      } ~
        path("getTestUser") {
          get {
            val res = UserService.getUser("tj123")
            println(res.get.toString)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Server basic persistence test</h1>"))
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

