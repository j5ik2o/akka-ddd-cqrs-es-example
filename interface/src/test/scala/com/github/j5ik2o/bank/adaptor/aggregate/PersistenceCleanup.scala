package com.github.j5ik2o.bank.adaptor.aggregate

import java.io.File

import akka.actor.ActorSystem
import org.apache.commons.io.FileUtils

import scala.util.Try

trait PersistenceCleanup {

  def system: ActorSystem

  val storageLocations = List("akka.persistence.journal.leveldb.dir",
                              "akka.persistence.journal.leveldb-shared.store.dir",
                              "akka.persistence.snapshot-store.local.dir").map { s =>
    new File(system.settings.config.getString(s))
  }

  def deleteStorageLocations(): Unit = {
    storageLocations.foreach(dir => Try(FileUtils.deleteDirectory(dir)))
  }

}
