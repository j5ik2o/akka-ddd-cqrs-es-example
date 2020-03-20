resolvers ++= Seq(
  "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
  "Seasar Repository" at "https://maven.seasar.org/maven2/",
  "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"
)

libraryDependencies ++= Seq(
  "com.h2database"  % "h2"         % "1.4.195",
  "commons-io"      % "commons-io" % "2.5",
  "org.seasar.util" % "s2util"     % "0.0.1"
)

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")

addSbtPlugin("jp.co.septeni-original" % "sbt-dao-generator" % "1.0.8")

addSbtPlugin("com.chatwork" % "sbt-wix-embedded-mysql" % "1.0.9")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.4.0")
