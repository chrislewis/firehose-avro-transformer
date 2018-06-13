import sbt.Keys._
import sbt._
import sbtrelease.Version

name := "firehose-avro-transformer"

resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.11.8"
releaseNextVersion := { ver => Version(ver).map(_.bumpMinor.string).getOrElse("Error") }
assemblyJarName in assembly := "firehose-avro-transformer.jar"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.192",
  "org.apache.avro" % "avro" % "1.7.7",
  "com.google.guava" % "guava" % "23.0"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings")
