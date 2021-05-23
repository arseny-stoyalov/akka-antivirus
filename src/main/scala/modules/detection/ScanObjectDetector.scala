package modules.detection

import akka.actor.{Actor, ActorRef, ActorSelection}
import modules.bypass.ResolveDirRequest
import modules.scanner.ScanRequest

import java.io.{FileInputStream, FileOutputStream, RandomAccessFile}
import java.nio.file.{Files, Path, Paths}
import java.util.zip.ZipInputStream

case class DetectionRequest(path: String, replyTo: ActorRef)

class ScanObjectDetector(dirResolver: ActorRef) extends Actor {

  val scanner: ActorSelection = context.actorSelection("user/Scanner")

  override def receive: Receive = {

    case r: DetectionRequest =>
      val p = Paths.get(r.path)
      println(s"${r.path} ${Files.isDirectory(p)}")
      resolveDir(p)

    case path: Path if !Files.isDirectory(path) =>
      val raf = new RandomAccessFile(path.toFile, "r")
      raf.readInt() match {
        case 0x504B0304 | 0x504B0506 | 0x504B0708 => processZip(path)
        case _ => resolveExecutable(path)
      }

  }

  def resolveDir(path: Path) =
    if (Files.isDirectory(path))
      dirResolver ! ResolveDirRequest(path, context.self)
    else context.self ! path

  def processZip(path: Path) = {

    val zis = new ZipInputStream(new FileInputStream(path.toFile))

    LazyList.continually(zis.getNextEntry).takeWhile(_ != null).foreach { file =>
      val fout = new FileOutputStream(file.getName)
      val buffer = new Array[Byte](1024)
      LazyList.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(fout.write(buffer, 0, _))
    }
    //https://www.baeldung.com/java-compress-and-uncompress

  }

  def resolveExecutable(path: Path) =
    if (path.toFile.canExecute)
      scanner ! ScanRequest(path)

}
