import akka.actor.ActorSystem
import configs.RootConfigs
import pureconfig.generic.auto._
import pureconfig.module.yaml._

object Main extends App {

  implicit val system: ActorSystem = ActorSystem(name = "scanner")

  val configs = {
    YamlConfigSource
      .file("src/main/resources/application.yaml")
      .load[RootConfigs]
      .fold(f => throw new Exception(f.prettyPrint()), s => s)
  }

  ActorStarter.setUpActors(configs)

}
