name := "arch"
version := "0.1"

lazy val akkaVersion = "2.5.3"
lazy val elastic4sVersion = "6.2.5"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "com.google.cloud" % "google-cloud-storage" % "1.24.1",         // GCS
  "org.neo4j.driver" % "neo4j-java-driver" % "1.4.4",             // Neo4j
  "ch.qos.logback" % "logback-classic" % "1.2.3",                 // Logging
  "org.json4s" %% "json4s-native" % "3.5.3",                      // JSON Parser
  "com.univocity" % "univocity-parsers" % "2.6.3",                // CSV Parser
  "com.newmotion" %% "akka-rabbitmq" % "5.0.0",                   // RabbitMQ
  "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.0",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test",
  "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion % "test",
  "com.sksamuel.elastic4s" %% "elastic4s-json4s" % elastic4sVersion
)