package utils

import org.bson.codecs.{Codec, DecoderContext}
import org.bson.{BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.collection.immutable.Document

object BsonParser {

  def decode[T](doc: Document, codec: Codec[T])(implicit decoderContext: DecoderContext): T = {
    val bson = BsonDocumentWrapper.asBsonDocument(doc, DEFAULT_CODEC_REGISTRY)
    val reader = new BsonDocumentReader(bson)
    codec.decode(reader, decoderContext)
  }

}
