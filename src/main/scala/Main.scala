import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import configs.RootConfigs
import pureconfig.generic.auto._
import pureconfig.module.yaml._

object Main extends App {

  val remoteConfig = ConfigFactory.load("akka-remote.conf")
  implicit val system: ActorSystem = ActorSystem(name = "antivirus-service", remoteConfig)

  val appConfigs = {
    YamlConfigSource
      .file("src/main/resources/application.yaml")
      .load[RootConfigs]
      .fold(f => throw new Exception(f.prettyPrint()), s => s)
  }

  ActorStarter.setUpActors(appConfigs)

}
