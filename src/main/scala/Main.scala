import akka.actor.ActorSystem
import configs.RootConfigs
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import services.mongo.MongoTemplate

object Main extends App {

  implicit val system: ActorSystem = ActorSystem(name = "scanner")

  val configs =
    ConfigSource
      .file("src/main/resources/application.properties")
      .load[RootConfigs]
      .fold(f => throw new Exception(f.prettyPrint()), s => s)

  ActorStarter.setUpActors(MongoTemplate(configs.mongo))

}
