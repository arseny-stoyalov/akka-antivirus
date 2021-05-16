package debug

import configs.RootConfigs
import model.MalwareObject
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
    "-45,1,49,49,0,-40,1,50,52,95,95,112,117,116,95,99,104,97,114,97,99,116,101,114,95,115,101,113,117,101,110,99,101,73,99,78,83,95,49,49,99,104,97,114,95,116,114,97,105,116,115,73,99,69,69,69,69,82,78,83,95,49,51,98,97,115,105,99,95,111,115,116,114,101,97,109,73,84,95,84,48,95,69,69,83,55,95,80,75,83,52,95,109,0,-66,2,3,4"
      .split(",")
      .map(_.toByte)

  def encode[T](obj: T)(implicit codec: Codec[T], encoderContext: EncoderContext) = {

    val doc = BsonDocument()
    val writer = new BsonDocumentWriter(doc)
    codec.encode(writer, obj, encoderContext)

    doc
  }

  val toInsert = MalwareObject("harder to find malware", signature, 100, 100)

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
