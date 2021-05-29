package modules.bypass

import akka.actor.{Actor, ActorRef}
import modules.detection.ScanObject

import java.nio.file.{Files, Path}

case class ResolveDirRequest(scanObject: ScanObject, replyTo: ActorRef)

class DirectoryResolver extends Actor {

  def resolve(scanObject: ScanObject, replyTo: ActorRef): Unit =
    if (Files.isDirectory(scanObject.path)) {
      val children = scanObject.path.toFile
        .listFiles()
      children.foreach(f => resolve(scanObject.copy(path = Path.of(f.getAbsolutePath)), replyTo))
    } else replyTo ! scanObject

  override def receive: Receive = {
    case r: ResolveDirRequest =>
      resolve(r.scanObject, r.replyTo)
  }

}
