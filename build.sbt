import sbt._
import sbtcrossproject.CrossPlugin.autoImport.{ CrossType, crossProject }

Global / onChangedBuildSource := ReloadOnSourceChanges

pluginCrossBuild / sbtVersion := "1.2.8"

inThisBuild(
  List(
    organization := "io.github.cquiroz",
    homepage := Some(url("https://github.com/cquiroz/sbt-locales")),
    licenses := Seq("BSD 3-Clause License" -> url("https://opensource.org/licenses/BSD-3-Clause")),
    developers := List(
      Developer("cquiroz",
                "Carlos Quiroz",
                "carlos.m.quiroz@gmail.com",
                url("https://github.com/cquiroz")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/cquiroz/sbt-locales"),
        "scm:git:git@github.com:cquiroz/sbt-locales.git"
      )
    )
  )
)

lazy val commonSettings = Seq(
  name := "sbt-locales",
  scalaVersion := "2.12.13",
  javaOptions ++= Seq("-Dfile.encoding=UTF8"),
  autoAPIMappings := true
)

def scalaNativeScala212Version(v: String) = if (v.startsWith("2.12.")) "2.12.13" else v

lazy val api = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("api"))
  .settings(commonSettings: _*)
  .settings(
    name := "cldr-api",
    scalaVersion := "2.12.13", // needs to match the version for sbt
    description := "scala-java-locales cldrl api",
    crossScalaVersions := Seq("2.13.5", "2.12.13", "2.13.4", "3.0.0-M3", "3.0.0-RC1"),
    libraryDependencies += "org.scalameta" %%% "munit" % "0.7.22" % Test,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies += ("org.portable-scala" %%% "portable-scala-reflect" % "1.1.1")
      .withDottyCompat(scalaVersion.value),
    Compile / doc / sources := { if (isDotty.value) Seq() else (Compile / doc / sources).value }
  )
  .jsSettings(scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)))
  .nativeSettings(
    scalaVersion ~= scalaNativeScala212Version,
    crossScalaVersions ~= { _.filter(_.startsWith("2.")).map(scalaNativeScala212Version) },
    skip in publish := true
  )

lazy val sbt_locales = project
  .in(file("sbt-locales"))
  .enablePlugins(SbtPlugin)
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "sbt-locales",
    description := "Sbt plugin to build custom locale databases",
    scalaVersion := "2.12.13",
    crossScalaVersions := Seq(),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    resources in Compile ++= (sources in (api.jvm, Compile)).value,
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      "com.eed3si9n"           %% "gigahorse-okhttp" % "0.5.0",
      "org.scala-lang.modules" %% "scala-xml"        % "1.3.0",
      "org.typelevel"          %% "cats-core"        % "2.5.0",
      "org.typelevel"          %% "cats-effect"      % "2.3.3",
      "com.eed3si9n"           %% "treehugger"       % "0.4.4"
    )
  )
  .dependsOn(api.jvm)

lazy val root = project
  .in(file("."))
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )
  .aggregate(api.js, api.jvm, api.native, sbt_locales)
