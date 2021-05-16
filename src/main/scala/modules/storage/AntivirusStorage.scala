package modules.storage

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import model.MalwareObject
import org.mongodb.scala.Observer
import org.mongodb.scala.bson.collection.immutable.Document
import services.mongo.MongoTemplate
import utils.BsonParser
import utils.implicits.Global.bsonDecoderContext

case object SignatureRequest

class AntivirusStorage(mongoTemplate: MongoTemplate) extends Actor with LazyLogging {

  private val signatures = mongoTemplate.collections.signatures

  override def receive: Receive = {

    case SignatureRequest =>
      signatures
        .find()
        .subscribe(new Observer[Document] {
          override def onNext(result: Document): Unit =
            sender() ! BsonParser.decode(result, MalwareObject.bsonCodec)

          override def onError(e: Throwable): Unit = logger.error("Failed to get a signature", e)

          override def onComplete(): Unit = logger.debug("Signatures loading finished")
        })

  }

}
