package modules.scanner

import akka.actor.{Actor, ActorRef}
import scala.collection.mutable.{Map => MutableMap}

case class ScanProcess(path: String, scanner: ActorRef)
case class StopFileScanProcess(path: String)
case class StopDirectoryScanProcess(path: String)

//Если кто-то это читает, то войдите в положение, времени было мало
class ScanController extends Actor {

  var processes: MutableMap[String, ActorRef] = MutableMap()

  override def receive: Receive = {

    case p: ScanProcess =>
      processes.addOne(p.path -> p.scanner)

    case r: StopFileScanProcess =>
      processes
        .get(r.path)
        .foreach { toStop =>
          context.stop(toStop)
          processes.remove(r.path)
        }

  }

}
