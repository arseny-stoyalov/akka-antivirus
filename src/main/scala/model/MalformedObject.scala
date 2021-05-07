package model

import io.circe.{Decoder, HCursor}
import org.bson.codecs.Codec
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.bson.codecs.Macros

case class MalformedObject(
  @BsonProperty("_id") id: String,
  name: String,
  length: Int,
  prefix: Array[Byte],
  hash: String,
  offsetStart: Long,
  offsetEnd: Long
)

object MalformedObject {

  val Empty: MalformedObject = MalformedObject("", "", 0, Array.emptyByteArray, "", 0, 0)

  private lazy val codecProvider = Macros.createCodecProvider[MalformedObject]()

  implicit val bsonCodec: Codec[MalformedObject] = codecProvider.get(classOf[MalformedObject], DEFAULT_CODEC_REGISTRY)

  implicit val decoder: Decoder[MalformedObject] = (c: HCursor) => {
    for {
      id <- c.downField("_id").as[String]
      name <- c.downField("name").as[String]
      length <- c.downField("length").as[Int]
      prefix <- c.downField("prefix").as[Array[Byte]]
      hash <- c.downField("hash").as[String]
      offsetStart <- c.downField("offsetStart").as[Int]
      offsetEnd <- c.downField("offsetEnd").as[Int]
    } yield {
      new MalformedObject(id, name, length, prefix, hash, offsetStart, offsetEnd)
    }
  }

}
