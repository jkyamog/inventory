name := """inventory"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

val playSlickVersion = "3.0.0"

libraryDependencies ++= Seq(
//  jdbc,
  cache,
  ws,
  guice,
  specs2 % Test,
  "com.typesafe.play" %% "play-json" % "2.6.3",
  "com.typesafe.play" %% "play-slick" % playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.h2database" % "h2" % "1.4.187"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// load different config on test
javaOptions in Test += "-Dconfig.file=conf/application-test.conf"
