import akka.actor.{ActorSystem, Props}
import modules.scanner.Scanner

object ActorStarter {

  def setUpActors(implicit system: ActorSystem): Unit =
    system.actorOf(Props[Scanner], "FileScanner")

}
