package modules.scanner

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import model.MalformedObject
import modules.storage.SignatureRequest

import java.nio.file.{Files, Paths}
import scala.annotation.tailrec

case class ScanRequest(path: String)

class Scanner extends Actor with LazyLogging {

  private val storage = context.actorSelection("user/AntivirusStorage")

  var path: String = ""

  override def receive: Receive = {
    case r: ScanRequest =>
      path = r.path
      storage ! SignatureRequest

    case next: MalformedObject =>
      val bytes = Files.readAllBytes(Paths.get(path))
      next.prefix.headOption match {
        case Some(_) => find(bytes, next.prefix, Array.emptyByteArray)
        case None => false
      }

  }

  @tailrec
  private def find(file: Array[Byte], prefix: Array[Byte], backupBuffer: Array[Byte]): Boolean =
    if (file.length == 0 || prefix.length == 0) {
      if (prefix.length == 0) true
      else false
    } else
      file.head match {
        case common if common == prefix.head =>
          if (backupBuffer.nonEmpty) find(file.drop(1), prefix.drop(1), backupBuffer)
          else find(file.drop(1), prefix.drop(1), prefix)
        case _ =>
          if (backupBuffer.nonEmpty) find(file.drop(1), backupBuffer, Array.emptyByteArray)
          else find(file.drop(1), prefix, backupBuffer)
      }

}
