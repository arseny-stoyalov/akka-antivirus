package modules.scanner

import akka.actor.{Actor, ActorSelection}

class ScanReportDelegator(receiver: ActorSelection) extends Actor {

  override def receive: Receive = {
    case r: ScanResponse => receiver ! r
  }

}
