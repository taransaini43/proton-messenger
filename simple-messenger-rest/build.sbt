ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "simple-messenger-rest"
  )

val AkkaVersion = "2.9.0"
val AkkaHttpVersion = "10.6.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  "com.typesafe.slick" %% "slick" % "3.4.1",
  "org.slf4j" % "slf4j-nop" % "2.0.5",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  "org.xerial" % "sqlite-jdbc" % "3.42.0.0",
  "io.spray" %%  "spray-json" % "1.3.6"

)

resolvers ++= Seq(
  "Spray repository" at "https://repo.spray.io",
  "Akka library repository" at "https://repo.akka.io/maven",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)