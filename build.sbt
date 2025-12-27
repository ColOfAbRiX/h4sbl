import org.typelevel.scalacoptions.ScalacOptions
import xerial.sbt.Sonatype._

// Project Information

val scala3Version = "3.7.4"

val caseInsensitiveVersion = "1.4.0"
val catsEffectVersion      = "3.5.4"
val catsVersion            = "2.12.0"
val fs2Version             = "3.9.3"
val http4sClientVersion    = "0.23.24"
val log4catsVersion        = "2.7.0"
val scodecBitsVersion      = "1.1.38"

// Global Settings

Global / run / fork              := true
Global / onChangedBuildSource    := ReloadOnSourceChanges
Global / tpolecatExcludeOptions ++= Set(ScalacOptions.warnUnusedLocals)
Test / tpolecatScalacOptions     := Set.empty

lazy val root =
  project
    .in(file("."))
    .settings(
      name         := "h4sbl",
      version      := "1.0.0",
      description  := "Http4s Better Logging",
      organization := "com.colofabrix.scala",
      scalaVersion := scala3Version,
      libraryDependencies ++= Seq(
        "co.fs2"        %% "fs2-core"           % fs2Version,
        "org.http4s"    %% "http4s-client"      % http4sClientVersion % Provided,
        "org.http4s"    %% "http4s-core"        % http4sClientVersion % Provided,
        "org.typelevel" %% "cats-core"          % catsVersion         % Provided,
        "org.typelevel" %% "cats-effect"        % catsEffectVersion   % Provided,
        "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion   % Provided,
        "org.scodec"    %% "scodec-bits"        % scodecBitsVersion,
        "org.typelevel" %% "case-insensitive"   % caseInsensitiveVersion,
        "org.typelevel" %% "log4cats-core"      % log4catsVersion,
        "org.typelevel" %% "log4cats-slf4j"     % log4catsVersion,
      ),
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
    )
    .settings(publishSettings)

// Publishing Settings

lazy val publishSettings =
  Seq(
    homepage             := Some(url("https://github.com/ColOfAbRiX/h4sbl")),
    startYear            := Some(2025),
    organizationName     := "ColOfAbRiX",
    organizationHomepage := Some(url("https://github.com/ColOfAbRiX")),
    licenses             := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ColOfAbRiX/h4sbl"),
        "scm:git@github.com:ColOfAbRiX/h4sbl.git",
      ),
    ),
    developers := List(
      Developer(
        "ColOfAbRiX",
        "Fabrizio Colonna",
        "colofabrix@tin.it",
        url("https://github.com/ColOfAbRiX"),
      ),
    ),
    pomIncludeRepository := { _ => false },
    publishMavenStyle    := true,
    sonatypeProjectHosting := Some(
      GitHubHosting("ColOfAbRiX", "h4sbl", "colofabrix@tin.it"),
    ),
    publishTo := {
      if (isSnapshot.value)
        Some(Opts.resolver.sonatypeOssSnapshots.head)
      else
        Some(Opts.resolver.sonatypeStaging)
    },

    // Scaladoc settings
    Compile / doc / scalacOptions ++= Seq(
      "-doc-title",
      "Http4s Better Logger API Documentation",
      "-doc-version",
      version.value,
      "-encoding",
      "UTF-8",
    ),
  )
