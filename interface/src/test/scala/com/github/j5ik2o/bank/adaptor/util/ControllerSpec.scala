package com.github.j5ik2o.bank.adaptor.util

import akka.http.scaladsl.model.{ HttpEntity, MediaTypes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKitBase
import akka.util.ByteString
import com.github.j5ik2o.scalatestplus.db.{ MySQLdConfig, UserWithPassword }
import com.wix.mysql.distribution.Version.v5_6_21
import io.circe.Encoder
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

import scala.concurrent.duration._

object ControllerSpec {

  implicit class JsonOps[A](val self: A) extends AnyVal {
    def toEntity(implicit enc: Encoder[A]): HttpEntity.Strict =
      HttpEntity(MediaTypes.`application/json`, ByteString(self.asJson.noSpaces))
  }

}

abstract class ControllerSpec
    extends FreeSpecLike
    with PropertyChecks
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with FlywayWithMySQLSpecSupport
    with ScalatestRouteTest
    with TestKitBase {
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(200, Millis))

  override def afterAll: Unit = cleanUp()

  override protected lazy val mySQLdConfig: MySQLdConfig = MySQLdConfig(
    version = v5_6_21,
    port = Some(12345),
    userWithPassword = Some(UserWithPassword("bank", "passwd")),
    timeout = Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble)
  )

}
