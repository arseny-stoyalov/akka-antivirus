import akka.actor.{Actor, ActorSystem, Props}
import configs.{MagicNumberConfig, RootConfigs}
import modules.bypass.DirectoryResolver
import modules.detection.{ScanObject, ScanObjectDetector, TempDirCleaner}
import modules.scanner.{ScanResponse, Scanner}
import modules.schedule.ScanScheduler
import services.mongo.MongoTemplate

import java.nio.file.{Files, Path, Paths}

object ActorStarter {

  class ScannerManager(magicNumberConfig: MagicNumberConfig) extends Actor {

    val temps: Path =
      Files.createTempDirectory(Paths.get("/Users/a1/Downloads/study"), ".")

    private val detector =
      context.actorOf(Props(classOf[ScanObjectDetector], magicNumberConfig, temps), "ScanObjectDetector")

    override def preStart(): Unit = {
      println("Initial message sent")
      detector ! ScanObject(Paths.get("/Users/a1/Downloads/study/executables"), None)
    }

    override def receive: Receive = {
      case r: ScanResponse =>
        println(r)
    }

  }

  def setUpActors(configs: RootConfigs)(implicit system: ActorSystem): Unit = {

    val mongoTemplate = MongoTemplate(configs.mongo)

    system.actorOf(Props(classOf[Scanner], mongoTemplate), "Scanner")
    system.actorOf(Props(classOf[ScannerManager], configs.magicNumbers), "ScannerManager") //todo: refactor
    system.actorOf(Props(classOf[DirectoryResolver]), "DirectoryResolver")
    system.actorOf(Props(classOf[TempDirCleaner], configs.tempDirCleaner), "TempDirCleaner")
    system.actorOf(Props(classOf[ScanScheduler], configs.magicNumbers), "ScanScheduler")

//    system.scheduler.scheduleOnce(5.millis)(initialActor ! "Stop")(system.dispatcher)

  }

}
