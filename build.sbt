name := "scanner"

version := "0.1"

scalaVersion := "2.13.5"

val akka = "2.6.14"

val mongo = "4.2.1"

val scalastic = "3.2.7"

val circe = "0.12.3"

val cats = new {
  val core = "2.1.1"
  val effects = "2.1.2"
}

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circe)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % cats.core,
  "org.typelevel" %% "cats-kernel" % cats.core,
  "org.typelevel" %% "cats-macros" % cats.core,
  "org.typelevel" %% "cats-effect" % cats.effects
)

libraryDependencies ++= Seq(
  "org.mongodb.scala" %% "mongo-scala-driver",
  "org.mongodb.scala" %% "mongo-scala-bson"
).map(_ % mongo)

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % scalastic,
  "org.scalatest" %% "scalatest" % scalastic % "test"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed",
  "com.typesafe.akka" %% "akka-remote"
).map(_ % akka)

libraryDependencies += "io.kaitai" % "kaitai-struct-runtime" % "0.9"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.15.0"

libraryDependencies += "com.github.pathikrit" % "better-files_2.13" % "3.9.1"
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"

libraryDependencies += "io.monix" %% "monix" % "3.1.0"
