import akka.actor.ActorSystem
import configs.RootConfigs
import modules.scanner.ScanRequest
import org.mongodb.scala.MongoClient
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Main extends App {

  implicit val system: ActorSystem = ActorSystem(name = "scanner")

  val configs =
    ConfigSource
      .file("src/main/resources/application.properties")
      .load[RootConfigs]
      .fold(f => throw new Exception(f.prettyPrint()), s => s)

  val mongoUri = configs.mongo.uri
  val dbName = configs.mongo.dbName
  val collectionName = configs.mongo.collection

  val client = MongoClient(mongoUri)
  val database = client.getDatabase(dbName)

  ActorStarter.setUpActors

  val fileScanner = system.actorSelection("user/FileScanner")

  fileScanner ! ScanRequest("App started")

}
