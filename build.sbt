import sbt.Package.ManifestAttributes

import sbtassembly.Log4j2MergeStrategy


ThisBuild / organization := "ru.fominmv"
ThisBuild / version      := "1.0.0"
ThisBuild / scalaVersion := "3.3.1"

ThisBuild / libraryDependencies ++= Seq(
    "com.github.scopt"         %% "scopt"      % "4.1.0",
    "org.apache.logging.log4j" %  "log4j-api"  % "2.20.0",
    "org.apache.logging.log4j" %  "log4j-core" % "2.20.0",
    "org.jline"                %  "jline"      % "3.23.0",
 )

ThisBuild / scalacOptions ++= Seq(
    "-Werror",
    "-feature",
    "-unchecked",
    "-deprecation",
)

ThisBuild / assemblyMergeStrategy := {
    case filename if filename endsWith "module-info.class" =>
        MergeStrategy.discard

    case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" =>
        Log4j2MergeStrategy.plugincache

    case filename =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(filename)
}

assembly / assemblyJarName := "" // Do not generate assembly file

lazy val client = project
    .dependsOn(core)
    .enablePlugins(BuildInfoPlugin)
    .settings(
        buildInfoPackage           := "ru.fominmv.simplechat.client",
        buildInfoKeys              := Seq(name, version),
        assembly / mainClass       := Some("ru.fominmv.simplechat.client.Main"),
        assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
        assembly / packageOptions  += ManifestAttributes("Multi-Release" -> "true"),
    )

lazy val server = project
    .dependsOn(core)
    .enablePlugins(BuildInfoPlugin)
    .settings(
        buildInfoPackage           := "ru.fominmv.simplechat.server",
        buildInfoKeys              := Seq(name, version),
        assembly / mainClass       := Some("ru.fominmv.simplechat.server.Main"),
        assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
        assembly / packageOptions  += ManifestAttributes("Multi-Release" -> "true"),
    )

lazy val core = project
    .settings(
        assembly / assemblyJarName := "", // Do not generate assembly file
        assembly / packageOptions  += ManifestAttributes("Multi-Release" -> "true"),
    )