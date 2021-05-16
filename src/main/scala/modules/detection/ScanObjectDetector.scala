package modules.detection

import akka.actor.Actor

case class DetectionRequest(path: String)

class ScanObjectDetector extends Actor {

  override def receive: Receive = {
    case r: DetectionRequest =>
      r.path
  }

}
