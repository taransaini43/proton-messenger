package docs.http.scaladsl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route.seal

import scala.io.StdIn

object ApplicationServer {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "messenger-system")
    implicit val executionContext = system.executionContext

    val route =
      path("pingtest") { // TODO : define all GET/POST routes here
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Server basic get ping response test</h1>"))
        }
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server now online. Please navigate to http://localhost:8080/pingtest\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
