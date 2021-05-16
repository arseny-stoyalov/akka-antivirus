import akka.actor.{Actor, ActorSystem, Props}
import modules.scanner.{ScanRequest, ScanResponse, Scanner}
import modules.storage.AntivirusStorage
import services.mongo.MongoTemplate

object ActorStarter {

  class InitialActor extends Actor {

    private val scanner = context.system.actorSelection("user/Scanner")

    override def preStart(): Unit = {
      println("Initial message sent")
      scanner ! ScanRequest("/Users/a1/Downloads/study/executables/main1.exe", context.self)
      scanner ! ScanRequest("/Users/a1/Downloads/study/executables/main2.exe", context.self)
      scanner ! ScanRequest("/Users/a1/Downloads/study/executables/main3.exe", context.self)
    }

    override def receive: Receive = {
      case r: ScanResponse =>
        println(r)
    }

  }

  def setUpActors(mongoTemplate: MongoTemplate)(implicit system: ActorSystem): Unit = {

    val scanner = system.actorOf(Props(classOf[Scanner], mongoTemplate), "Scanner")
    system.actorOf(Props(classOf[AntivirusStorage], mongoTemplate), "AntivirusStorage")
    val initial = system.actorOf(Props(classOf[InitialActor]), "InitialActor")
    println(s"Scanner: ${scanner.path} Initial: ${initial.path}")

  }

}
