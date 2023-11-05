package docs.http.scaladsl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import service.UserService

import scala.io.StdIn
import scala.util.Try

import spray.json._

object ApplicationServer  {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "messenger-system")
    implicit val executionContext = system.executionContext

    val config = ConfigFactory.load()
    UserService.init()

    lazy val serviceHost : String = Try(config.getString("service.host")).getOrElse("localhost")
    lazy val servicePort : Int = Try(config.getInt("service.port")).getOrElse(8080)

    val routes = {

      // test routes
      path("pingtest") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Server basic get ping response test</h1>"))
        }
      }~ path("createTestUser") {
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


      // legit routes
    }~ pathPrefix("create") {
          path("user") {
            post {
              entity(as[String]) { inputStr =>
                println(inputStr)
                val inputJson = inputStr.parseJson
                complete(HttpEntity(ContentTypes.`application/json`, inputStr))
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
              // val userInput = json.convertTo[UserInput]
              // Better way is to parse via case classes - defined UserInput below this class for reference
              val uname = json.asInstanceOf[JsObject].fields.get("username").getOrElse("").toString
              val pcode = json.asInstanceOf[JsObject].fields.get("passcode").getOrElse("").toString

              UserService.getUser(uname) match {
                case Some(u) => // found existing user
                case None => // no matching user
              }

              complete(HttpEntity(ContentTypes.`application/json`, inputStr))
            }
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

