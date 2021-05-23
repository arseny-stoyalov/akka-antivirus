package modules.scanner

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.LazyLogging
import model.MalwareObject
import modules.scanner.Scanner.isInfected
import org.mongodb.scala.Observer
import org.mongodb.scala.bson.collection.immutable.Document
import services.mongo.MongoTemplate
import utils.BsonParser
import utils.UtilFunctions.md5
import utils.implicits.Global.bsonDecoderContext

import java.nio.file.{Files, Paths}
import scala.annotation.tailrec

case class ScanRequest(path: String, replyTo: ActorRef)
case class ScanResponse(path: String, matchedMalware: Boolean, malwareName: Option[String])

class Scanner(mongoTemplate: MongoTemplate) extends Actor with LazyLogging {

  private val signatures = mongoTemplate.collections.signatures

  override def receive: Receive = {
    case r: ScanRequest =>
      val bytes = Files.readAllBytes(Paths.get(r.path))

      signatures
        .find()
        .subscribe(new Observer[Document] {
          override def onNext(result: Document): Unit = {
            val next = BsonParser.decode(result, MalwareObject.bsonCodec)
            if (next.prefix.nonEmpty) {
              val trimmedByOffsets = bytes.drop(next.offsetStart).dropRight(next.offsetEnd)
              r.replyTo ! ScanResponse(r.path, isInfected(trimmedByOffsets, next), Some(next.name))
            }
          }

          override def onError(e: Throwable): Unit = logger.error("Signatures scanning failed", e)

          override def onComplete(): Unit =
            logger.debug("Signatures scanning finished")
        })

  }

}

object Scanner {

  @tailrec
  final def isInfected(bytes: Array[Byte], malware: MalwareObject): Boolean =
    if (bytes.isEmpty) false
    else {
      val matchedStartIndex = findSignatureIndex(bytes, malware.prefix, Array.emptyByteArray, 0)
      matchedStartIndex match {
        case Some(start) =>
          val end = start + malware.length
          val matched = bytes.slice(start, end)
          if (md5(matched) == malware.hash)
            true
          else isInfected(bytes.drop(start + malware.prefix.length), malware)
        case None => false
      }
    }

  @tailrec
  final def findSignatureIndex(
    file: Array[Byte],
    prefix: Array[Byte],
    backupBuffer: Array[Byte],
    index: Int
  ): Option[Int] =
    if (file.length == 0 || prefix.length == 0) {
      if (prefix.length == 0) Some(index)
      else None
    } else {
      file.head match {
        case common if common == prefix.head =>
          if (backupBuffer.nonEmpty) findSignatureIndex(file.drop(1), prefix.drop(1), backupBuffer, index)
          else findSignatureIndex(file.drop(1), prefix.drop(1), prefix, index)
        case _ =>
          if (backupBuffer.nonEmpty)
            findSignatureIndex(
              file.drop(1),
              backupBuffer,
              Array.emptyByteArray,
              index + 1 + (backupBuffer.length - prefix.length)
            )
          else findSignatureIndex(file.drop(1), prefix, backupBuffer, index + 1)
      }
    }

}
