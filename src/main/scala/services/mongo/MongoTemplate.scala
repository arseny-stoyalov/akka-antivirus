package services.mongo

import com.typesafe.scalalogging.LazyLogging
import configs.MongoConfigs
import monix.execution.Scheduler
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class MongoTemplate(db: MongoDatabase) {

  trait CollectionSet

  val collections = new CollectionSet {

    val signatures: MongoCollection[Document] = db.getCollection("signatures")

  }

}

object MongoTemplate extends LazyLogging {

//  implicit class CollectionExtension[T](collection: MongoCollection[T]) {
//
//    def countDocs(): Long = {
//      val future = collection
//        .countDocuments()
//        .head()
//      Await.result(future, 10.seconds)
//    }
//
//  }

  def apply(configs: MongoConfigs): MongoTemplate = {
    val mongoUri = configs.uri
    val dbName = configs.dbName

    val client = MongoClient(mongoUri)
    val database = client.getDatabase(dbName)

    new MongoTemplate(database)
  }

}
