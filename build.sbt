organization  := "com.github.bmorris458"

version       := "0.12.2"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.6"
  val sprayV = "1.3.3"
  val scalatestV = "2.2.6"
  Seq(
    "io.spray"                 %% "spray-can"        % sprayV,
    "io.spray"                 %% "spray-routing"    % sprayV,
    "io.spray"                 %% "spray-testkit"    % sprayV  % "test",
    "com.typesafe.akka"        %% "akka-actor"       % akkaV,
    "com.typesafe.akka"        %% "akka-persistence" % akkaV,
    "com.typesafe.akka"        %% "akka-testkit"     % akkaV   % "test",
    "org.scalactic"            %% "scalactic"        % scalatestV,
    "org.scalatest"            %% "scalatest"        % scalatestV % "test",
    "commons-io"                % "commons-io"       % "2.4",
    //    "org.scalaz"               %% "scalaz-core"      % "7.1.0",
    "org.specs2"               %% "specs2-core"      % "2.3.11" % "test",
    "org.iq80.leveldb"          % "leveldb"          % "0.7",
    "org.fusesource.leveldbjni" % "leveldbjni-all"   % "1.8"
  )
}

Revolver.settings

fork := true
