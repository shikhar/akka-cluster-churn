import sbt._
import sbt.Keys._

object Build extends sbt.Build {

  lazy val p = Project(
    id = "akka-cluster-churn",
    base = file("."),
    settings = Defaults.defaultSettings
  ).settings(
      organization := "net.schmizz",
      scalaVersion := "2.11.1",
      version := "0.1.0",
      scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
    ).settings(
    ).settings(
      resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.typesafeRepo("releases")),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % v.akka,
        "com.typesafe.akka" %% "akka-cluster" % v.akka
      )
    )

  object v {
    val akka = "2.3.4"
  }

}
