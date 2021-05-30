package modules.scanner

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.LazyLogging
import model.MalwareObject
import modules.scanner.Scanner.isInfected
import org.mongodb.scala.Observer
import org.mongodb.scala.bson.collection.immutable.Document
import services.mongo.MongoTemplate
import source.ScanObjectSource
import utils.BsonParser
import utils.UtilFunctions.{md5, using}
import utils.implicits.Global.bsonDecoderContext

import java.io.FileInputStream
import java.nio.file.Path
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

case class ScanRequest(path: Path, zipFileRef: Option[Path], replyTo: ActorRef, source: ScanObjectSource)
case class ScanResponse(
  path: Path,
  matchedMalware: Boolean,
  malwareName: String,
  replyTo: ActorRef,
  source: ScanObjectSource
)

class Scanner(mongoTemplate: MongoTemplate, receiver: ActorRef) extends Actor with LazyLogging {

  private val signatures = mongoTemplate.collections.signatures

  override def receive: Receive = {
    case r: ScanRequest =>
      signatures
        .find()
        .subscribe(new Observer[Document] {
          override def onNext(result: Document): Unit = {
            //todo: Для тестов
//            Thread.sleep(10.seconds.toMillis)
            val next = BsonParser.decode(result, MalwareObject.bsonCodec)
            if (next.prefix.nonEmpty) {
              using(new FileInputStream(r.path.toFile)) { in =>
                in.skip(next.offsetStart)
                val bytes = in.readNBytes(next.offsetEnd - next.offsetStart)
                r.zipFileRef.fold(
                  receiver ! ScanResponse(r.path, isInfected(bytes, next), next.name, r.replyTo, r.source)
                )(p => receiver ! ScanResponse(p, isInfected(bytes, next), next.name, r.replyTo, r.source))
              } {
                case e: Exception =>
                  logger.error(s"Failed to scan for ${next.name} in ${r.path.getFileName}", e)
              }
            }
          }

          override def onError(e: Throwable): Unit = logger.error("Signature scanning failed", e)

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
