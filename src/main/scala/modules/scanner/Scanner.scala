package modules.scanner

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.LazyLogging
import model.MalwareObject
import org.mongodb.scala.Observer
import org.mongodb.scala.bson.collection.immutable.Document
import services.mongo.MongoTemplate
import utils.BsonParser
import utils.implicits.Global.bsonDecoderContext

import java.nio.file.{Files, Paths}
import scala.annotation.tailrec

case class ScanRequest(path: String, replyTo: ActorRef)
case class ScanResponse(path: String, matchedMalware: Boolean, malwareName: Option[String])

class Scanner(mongoTemplate: MongoTemplate) extends Actor with LazyLogging {

  private val signatures = mongoTemplate.collections.signatures

  override def receive: Receive = {
    case r: ScanRequest =>
      logger.debug(s"Got request from ${sender().path}")
      val bytes = Files.readAllBytes(Paths.get(r.path))

      signatures
        .find()
        .subscribe(new Observer[Document] {
          override def onNext(result: Document): Unit = {
            val next = BsonParser.decode(result, MalwareObject.bsonCodec)
            if (next.prefix.nonEmpty) {
              val trimmedByOffsets = bytes.drop(next.offsetStart).dropRight(next.offsetEnd)
              val matched = find(trimmedByOffsets, next.prefix, Array.emptyByteArray)
              r.replyTo ! ScanResponse(r.path, matched, Some(next.name))
            }
          }

          override def onError(e: Throwable): Unit = logger.error("Failed to get a signature", e)

          override def onComplete(): Unit =
            logger.debug("Signatures scanning finished")
        })

    case _: String => context.stop(context.self)

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
