package modules.scanner

import akka.actor.Actor

case class ScanRequest(path: String)

class Scanner extends Actor {

  override def receive: Receive = {
    case r: ScanRequest =>
  }

}
