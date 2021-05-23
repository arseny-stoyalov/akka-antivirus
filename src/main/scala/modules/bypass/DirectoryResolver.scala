package modules.bypass

import akka.actor.{Actor, ActorRef}

import java.nio.file.{Files, Path}

case class ResolveDirRequest(path: Path, replyTo: ActorRef)
case class ResolvedFile(path: Path)

class DirectoryResolver extends Actor {

  def resolve(path: Path, replyTo: ActorRef): Unit =
    if (Files.isDirectory(path))
      path.toFile
        .listFiles()
        .foreach(f => resolve(Path.of(f.getAbsolutePath), replyTo))
    else replyTo ! ResolvedFile(path)

  override def receive: Receive = {
    case r: ResolveDirRequest =>
      resolve(r.path, r.replyTo)
  }

}
