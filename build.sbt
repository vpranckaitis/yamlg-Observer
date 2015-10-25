lazy val commonSettings = Seq(
  organization := "lt.vpranckaitis",
  name := "yamlg-observer",
  version := "0.1.0",
  scalaVersion := "2.11.5",
  scalacOptions += "-optimize",
  assemblyJarName in assembly := "yamlg-observer.jar"
)

lazy val repositories =  Seq(
	resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk",
	resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

lazy val dependencies = Seq(
  libraryDependencies += "redis.clients" % "jedis" % "2.6.2",
  
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  
  libraryDependencies += "io.spray" %% "spray-routing" % "1.3.2",
  libraryDependencies += "io.spray" %% "spray-can" % "1.3.2",
  libraryDependencies += "io.spray" %% "spray-client" % "1.3.1",
  libraryDependencies += "io.spray" %% "spray-json" % "1.3.1",
  libraryDependencies += "io.spray" %% "spray-httpx" % "1.3.2",
  libraryDependencies += "io.spray" %% "spray-util" % "1.3.2",
  libraryDependencies += "org.scalaj" %% "scalaj-http" % "1.1.4",
  libraryDependencies += "org.sedis" %% "sedis" % "1.2.2"
)

lazy val dependenciesTest = Seq(
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
)

lazy val CompleteTest = config("complete") extend(Test)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(repositories: _*).
  settings(dependencies: _*).
  settings(dependenciesTest: _*).
  configs(CompleteTest).
  settings( inConfig(CompleteTest)(Defaults.testTasks) : _*).
  settings(
    testOptions in Test := Seq(Tests.Argument("-l", "lt.vpranckaitis.scalatest.tags.IntegrationTest")),
    testOptions in CompleteTest := Seq()
  )
