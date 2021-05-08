package debug

import configs.RootConfigs
import model.MalformedObject
import org.bson.BsonDocumentWriter
import org.bson.codecs.{Codec, EncoderContext}
import org.mongodb.scala.{MongoClient, Observer}
import org.mongodb.scala.bson.BsonDocument
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import utils.UtilFunctions.md5
import utils.implicits.Global.bsonEncoderContext

import java.nio.file.{Files, Paths}

object DebugPoint extends App {

  val signature =
    "48 89 45 D8 E8 73 15 00 00 31 C9 89 CA C7 45 EC 03 00 00 00 48 8D 7D F0 48 8D 75 EC 48 89 45 D0 E8 E5 01 00 00"
      .split(" ")
      .map(Integer.parseInt(_, 16).toByte)

  def encode[T](obj: T)(implicit codec: Codec[T], encoderContext: EncoderContext) = {

    val doc = BsonDocument()
    val writer = new BsonDocumentWriter(doc)
    codec.encode(writer, obj, encoderContext)

    doc
  }

  val toInsert = MalformedObject("test virus", signature, 10082, 10314)

  val configs =
    ConfigSource
      .file("src/main/resources/application.properties")
      .load[RootConfigs]
      .fold(f => throw new Exception(f.prettyPrint()), s => s)

  val mongoUri = configs.mongo.uri
  val dbName = configs.mongo.dbName

  val client = MongoClient(mongoUri)
  val database = client.getDatabase(dbName)

  database
    .getCollection("signatures")
    .insertOne(encode(toInsert))
    .subscribe(ex => println(ex.getMessage), () => println("success"))

  Thread.sleep(5_000)

}
