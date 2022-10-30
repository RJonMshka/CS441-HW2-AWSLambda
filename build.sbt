ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "LogProcessorLambda"
  )

val typesafeConfigVersion = "1.4.2"
val scalacticVersion = "3.2.9"
val awsSdkS3Version = "1.12.90"
val awsLambdaJavaCoreVersion = "1.2.1"
val awsLambdaJavaEventsVersion = "3.11.0"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % typesafeConfigVersion,
  "org.scalactic" %% "scalactic" % scalacticVersion,
  "org.scalatest" %% "scalatest" % scalacticVersion % Test,
  "org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test,
  "com.amazonaws" % "aws-java-sdk-s3" % awsSdkS3Version,
  "com.amazonaws" % "aws-lambda-java-core" % awsLambdaJavaCoreVersion,
  "com.amazonaws" % "aws-lambda-java-events" % awsLambdaJavaEventsVersion
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
