import akka.actor.{ActorSystem, Props}
import modules.scanner.Scanner
import modules.storage.AntivirusStorage
import services.mongo.MongoTemplate

object ActorStarter {

  def setUpActors(mongoTemplate: MongoTemplate)(implicit system: ActorSystem): Unit = {

    system.actorOf(Props(classOf[Scanner]), "Scanner")
    system.actorOf(Props(classOf[AntivirusStorage], mongoTemplate), "AntivirusStorage")

  }

}
