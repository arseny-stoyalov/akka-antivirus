package debug

import configs.RootConfigs
import model.MalwareObject
import org.bson.BsonDocumentWriter
import org.bson.codecs.{Codec, EncoderContext}
import org.mongodb.scala.{MongoClient, Observer}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.collection.immutable.Document
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import utils.BsonParser
import utils.UtilFunctions.md5
import utils.implicits.Global.{bsonDecoderContext, bsonEncoderContext}

import java.nio.file.{Files, Paths}

object DebugPoint extends App {

  val signature =
    "405F5F5A 4E537433 5F5F3131 31636861 725F7472 61697473 49634533 656F6645 76009040 5F5F5A4E 5374335F 5F313131 63686172 5F747261 69747349 6345366C 656E6774 6845504B 63009040 5F5F5A4E 5374335F 5F313131 74686973 5F746872 65616439 736C6565 705F666F 7249784E 535F3572 6174696F 494C6C31 454C6C31 45454545 4576524B 4E535F36 6368726F 6E6F3864 75726174 696F6E49 545F5430 5F454500 80089040 5F5F5A4E 5374335F 5F313234 5F5F7075 745F6368 61726163 7465725F 73657175 656E6365 49634E53 5F313163 6861725F 74726169"
      .split(" ")
      .flatMap(set => Array(set.take(2), set.slice(2, 4), set.slice(4, 6), set.takeRight(2)))
      .map(Integer.parseInt(_, 16).toByte)

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

//  database
//    .getCollection("signatures")
//    .find()
//    .subscribe(new Observer[Document] {
//      override def onNext(result: Document): Unit = {
//        val obj = BsonParser.decode(result, MalwareObject.bsonCodec)
//        println(s"${obj.name} | ${obj.prefix.map("%02X" format _).mkString(",")}")
//      }
//
//      override def onError(e: Throwable): Unit = ()
//
//      override def onComplete(): Unit = ()
//    })

  Thread.sleep(5_000)

}
