package com.github.j5ik2o.bank.adaptor.serialization

import akka.actor.ExtendedActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.serialization.SerializerWithStringManifest
import com.github.j5ik2o.bank.domain.model._
import org.slf4j.LoggerFactory
import pureconfig._

object BankAccountEventJSONManifest {
  final val CREATE   = BankAccountOpened.getClass.getName.stripSuffix("$")
  final val UPDATE   = BankAccountEventUpdated.getClass.getName.stripSuffix("$")
  final val DEPOSIT  = BankAccountDeposited.getClass.getName.stripSuffix("$")
  final val WITHDRAW = BankAccountWithdrawn.getClass.getName.stripSuffix("$")
  final val DESTROY  = BankAccountClosed.getClass.getName.stripSuffix("$")
}

class BankAccountEventJSONSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import BankAccountCreatedJson._
  import BankAccountEventJSONManifest._
  import io.circe.generic.auto._

  private val logger = LoggerFactory.getLogger(getClass)

  private val config = loadConfigOrThrow[BankAccountEventJSONSerializerConfig](
    system.settings.config.getConfig("bank.interface.bank-account-event-json-serializer")
  )

  private implicit val log: LoggingAdapter = Logging.getLogger(system, getClass)

  private val isDebugEnabled = config.isDebuged

  override def identifier: Int = 50

  override def manifest(o: AnyRef): String = {
    val result = o.getClass.getName
    logger.debug(s"manifest: $result")
    result
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case orig: BankAccountOpened       => CirceJsonSerialization.toBinary(orig, isDebugEnabled)
    case orig: BankAccountEventUpdated => CirceJsonSerialization.toBinary(orig, isDebugEnabled)
    case orig: BankAccountDeposited    => CirceJsonSerialization.toBinary(orig, isDebugEnabled)
    case orig: BankAccountWithdrawn    => CirceJsonSerialization.toBinary(orig, isDebugEnabled)
    case orig: BankAccountClosed       => CirceJsonSerialization.toBinary(orig, isDebugEnabled)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    logger.debug(s"fromBinary: $manifest")
    manifest match {
      case CREATE =>
        CirceJsonSerialization.fromBinary[BankAccountOpened, BankAccountCreatedJson](bytes, isDebugEnabled)
      case UPDATE =>
        CirceJsonSerialization.fromBinary[BankAccountEventUpdated, BankAccountUpdatedJson](bytes, isDebugEnabled)
      case DEPOSIT =>
        CirceJsonSerialization.fromBinary[BankAccountDeposited, BankAccountDepositedJson](bytes, isDebugEnabled)
      case WITHDRAW =>
        CirceJsonSerialization.fromBinary[BankAccountWithdrawn, BankAccountWithdrawedJson](bytes, isDebugEnabled)
      case DESTROY =>
        CirceJsonSerialization.fromBinary[BankAccountClosed, BankAccountDestroyedJson](bytes, isDebugEnabled)
    }
  }
}
