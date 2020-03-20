import org.seasar.util.lang.StringUtil

import scala.concurrent.duration._
import Dependencies._
import Utils._

val baseSettings = Seq(
  organization := "com.github.j5ik2o",
  version := "0.0.1",
  scalaVersion := "2.12.4",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding",
    "UTF-8",
    "-Xfatal-warnings",
    "-language:existentials",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:higherKinds",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-Xmax-classfile-name",
    "200"
  ),
  resolvers ++= Seq(
    Resolver.jcenterRepo,
    "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
    "Seasar Repository" at "https://maven.seasar.org/maven2/",
    "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"
  ),
  libraryDependencies ++= Seq(
    Hashids.hashids,
    Sisioh.baseUnits,
    TypeSafe.config,
    Slf4J.api,
    Enumeratum.enumeratum,
    Logback.classic       % Test,
    ScalaTest.scalactic   % Test,
    ScalaTest.scalaTest   % Test,
    ScalaCheck.scalaCheck % Test
  )
)

val `infrastructure` = (project in file("infrastructure"))
  .settings(baseSettings)
  .settings(
    name := "bank-infrastructure",
    libraryDependencies ++= Circe.all,
    libraryDependencies ++= Seq(
      TypeSafe.Akka.slf4j,
      TypeSafe.Akka.actor,
      TypeSafe.Akka.stream,
      TypeSafe.Akka.cluster,
      TypeSafe.Akka.clusterTools,
      TypeSafe.Akka.clusterSharding,
      PureConfig.pureConfig,
      Monocle.monocleCore,
      Monocle.monocleMacro,
      Monocle.monocleLaw,
      TypeSafe.Akka.streamTestKit % Test,
      TypeSafe.Akka.testKit       % Test
    )
  )

val dbDriver   = "com.mysql.jdbc.Driver"
val dbName     = "bank"
val dbUser     = "bank"
val dbPassword = "passwd"
val dbPort     = RandomPortSupport.temporaryServerPort()
val dbUrl      = s"jdbc:mysql://localhost:$dbPort/$dbName?useSSL=false"

val `flyway` = (project in file("tools/flyway"))
  .settings(baseSettings)
  .settings(
    name := "bank-flyway",
    libraryDependencies ++= Seq(MySQL.connectorJava),
    parallelExecution in Test := false,
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(dbPort),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    //wixMySQLTempPath := Some(sys.env("HOME") + "/.wixMySQL/work"),
    wixMySQLTimeout := Some(2 minutes),
    flywayDriver := dbDriver,
    flywayUrl := dbUrl,
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
      s"filesystem:${baseDirectory.value}/src/test/resources/db-migration/",
      s"filesystem:${baseDirectory.value}/src/test/resources/db-migration/test"
    ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
      "engineName"                 -> "MEMORY",
      "idSequenceNumberEngineName" -> "MyISAM"
    ),
    flywayMigrate := (flywayMigrate dependsOn wixMySQLStart).value
  )

lazy val localMysql = (project in file("tools/local-mysql"))
  .settings(baseSettings)
  .settings(
    name := "bank-local-mysql",
    libraryDependencies ++= Seq(MySQL.connectorJava),
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(3306),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    wixMySQLTimeout := Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble),
    flywayDriver := dbDriver,
    flywayUrl := s"jdbc:mysql://localhost:3306/$dbName?useSSL=false",
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
      s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/db-migration/",
      s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/db-migration/test",
      s"filesystem:${baseDirectory.value}/src/main/resources/dummy-migration"
    ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
      "engineName"                 -> "InnoDB",
      "idSequenceNumberEngineName" -> "MyISAM"
    ),
    run := (flywayMigrate dependsOn wixMySQLStart).value
  )

lazy val migrate = (project in file("tools/migrate"))
  .settings(baseSettings)
  .settings(
    name := "bank-migrate",
    flywayDriver := dbDriver,
    flywayUrl := s"jdbc:mysql://${scala.util.Properties
      .propOrNone("mysql.host")
      .getOrElse("localhost")}:${scala.util.Properties.propOrNone("mysql.port").getOrElse("3306")}/$dbName?useSSL=false",
    flywayUser := scala.util.Properties.propOrNone("mysql.user").getOrElse(dbUser),
    flywayPassword := scala.util.Properties.propOrNone("mysql.password").getOrElse(dbPassword),
    flywaySchemas := scala.util.Properties.propOrNone("mysql.schemas").map(_.split(",").toSeq).getOrElse(Seq(dbName)),
    flywayLocations := {
      Seq(
        s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/rdb-migration/",
        s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/rdb-migration/test",
        s"filesystem:${(baseDirectory in localMysql).value}/src/main/resources/dummy-migration"
      )
    },
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
      "engineName"                 -> "InnoDB",
      "idSequenceNumberEngineName" -> "MyISAM"
    ),
    run := (flywayMigrate).value
  )

val `domain` = (project in file("domain"))
  .settings(baseSettings)
  .settings(
    name := "bank-domain"
  )
  .dependsOn(`infrastructure`)

val `use-case` = (project in file("use-case"))
  .settings(baseSettings)
  .settings(
    name := "bank-use-case",
    parallelExecution in Test := false
  )
  .dependsOn(`domain`)

val `interface` = (project in file("interface"))
  .enablePlugins(MultiJvmPlugin)
  .settings(baseSettings)
  .settings(
    name := "bank-interface",
    libraryDependencies ++= Seq(
      MySQL.connectorJava,
      TypeSafe.Slick.slick,
      TypeSafe.Slick.slickHikariCP,
      TypeSafe.Akka.persistence,
      TypeSafe.Akka.http,
      Heikoseeberger.akkaHttpCirce,
      Dnvriend.akkaPersistenceJBDC,
      LevelDB.levelDB,
      LevelDB.levelDBJNIAll,
      TypeSafe.Akka.testKit          % Test,
      TypeSafe.Akka.multiNodeTestKit % Test,
      TypeSafe.Akka.httpTestKit      % Test,
      Commons.io                     % Test,
      J5ik2o.scalaTestPlusDB         % Test,
      Logback.classic                % Test
    ),
    parallelExecution in Test := false,
    // fork in Test := true,
    // JDBCのドライバークラス名を指定します(必須)
    driverClassName in generator := dbDriver,
    // JDBCの接続URLを指定します(必須)
    jdbcUrl in generator := dbUrl,
    // JDBCの接続ユーザ名を指定します(必須)
    jdbcUser in generator := dbUser,
    // JDBCの接続ユーザのパスワードを指定します(必須)
    jdbcPassword in generator := dbPassword,
    // カラム型名をどのクラスにマッピングするかを決める関数を記述します(必須)
    propertyTypeNameMapper in generator := {
      case "INTEGER" | "TINYINT"             => "Int"
      case "BIGINT"                          => "Long"
      case "VARCHAR"                         => "String"
      case "BOOLEAN" | "BIT"                 => "Boolean"
      case "DATE" | "TIMESTAMP" | "DATETIME" => "java.time.ZonedDateTime"
      case "DECIMAL"                         => "BigDecimal"
      case "ENUM"                            => "String"
    },
    propertyNameMapper in generator := {
      case "type"     => "`type`"
      case columnName => StringUtil.decapitalize(StringUtil.camelize(columnName))
    },
    tableNameFilter in generator := { tableName: String =>
      tableName.toUpperCase match {
        case "SCHEMA_VERSION"                      => false
        case t if t.endsWith("ID_SEQUENCE_NUMBER") => false
        case "JOURNAL" | "SNAPSHOT"                => false
        case _                                     => true
      }
    },
    outputDirectoryMapper in generator := {
      case s if s.endsWith("Spec") => (sourceDirectory in Test).value
      case s =>
        new java.io.File((scalaSource in Compile).value, "/com/github/j5ik2o/bank/adaptor/dao")
    },
    // モデル名に対してどのテンプレートを利用するか指定できます。
    templateNameMapper in generator := {
      case className if className.endsWith("Spec") => "template_spec.ftl"
      case _                                       => "template.ftl"
    },
    compile in Compile := ((compile in Compile) dependsOn (generateAll in generator)).value,
    generateAll in generator := Def
      .taskDyn {
        val ga = (generateAll in generator).value
        Def
          .task {
            (wixMySQLStop in flyway).value
          }
          .map(_ => ga)
      }
      .dependsOn(flywayMigrate in flyway)
      .value,
    compile in MultiJvm := (compile in MultiJvm).triggeredBy(compile in Test).value,
    executeTests in Test := Def.task {
      val testResults      = (executeTests in Test).value
      val multiNodeResults = (executeTests in MultiJvm).value
      val overall = (testResults.overall, multiNodeResults.overall) match {
        case (TestResult.Passed, TestResult.Passed) => TestResult.Passed
        case (TestResult.Error, _)                  => TestResult.Error
        case (_, TestResult.Error)                  => TestResult.Error
        case (TestResult.Failed, _)                 => TestResult.Failed
        case (_, TestResult.Failed)                 => TestResult.Failed
      }
      Tests.Output(overall,
                   testResults.events ++ multiNodeResults.events,
                   testResults.summaries ++ multiNodeResults.summaries)
    }.value,
    assemblyMergeStrategy in (MultiJvm, assembly) := {
      case "application.conf" => MergeStrategy.concat
      case "META-INF/aop.xml" => MergeStrategy.concat
      case x =>
        val old = (assemblyMergeStrategy in (MultiJvm, assembly)).value
        old(x)
    }
  )
  .configs(MultiJvm)
  .dependsOn(`use-case`)

val `api-server` = (project in file("api-server"))
  .settings(baseSettings)
  .settings(
    name := "bank-api-server",
    libraryDependencies ++= Seq(
      Logback.classic
    )
  )
  .dependsOn(`interface`, `domain`)

val `read-model-updater` = (project in file("read-model-updater"))
  .settings(baseSettings)
  .settings(
    name := "bank-read-model-updater",
    libraryDependencies ++= Seq(
      Logback.classic
    )
  )
  .dependsOn(`interface`, `domain`)

val `bank` = (project in file("."))
  .settings(baseSettings)
  .settings(
    name := "bank"
  )
  .aggregate(`interface`, `domain`, `flyway`)
