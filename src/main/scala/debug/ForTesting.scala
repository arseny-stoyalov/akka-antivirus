package debug

import org.apache.commons.io.filefilter.MagicNumberFileFilter
import org.apache.commons.io.{FileUtils, FilenameUtils}
import utils.UtilFunctions.using

import java.io.{File, FileInputStream, FileOutputStream, RandomAccessFile}
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}
import java.util.Comparator
import java.util.zip.{ZipEntry, ZipFile, ZipInputStream}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object ForTesting extends App {

  def processZip(path: Path) = {
    import scala.jdk.StreamConverters._

    val name = FilenameUtils.removeExtension(path.getFileName.toString)
    val temp = Files.createTempDirectory(path.getParent, ".scanner_")

    unzip(path, temp)

    val unzipped = temp.resolve(name)

    temp.toFile.listFiles().foreach { f =>
      val p = Paths.get(f.getAbsolutePath)
      if (p != unzipped) Files.walk(p).sorted(Comparator.reverseOrder[Path]()).toScala(List).foreach(Files.delete)
    }

    println(unzipped.getFileName)

  }

  def unzip(zipPath: Path, outputPath: Path): Unit =
    using(new ZipFile(zipPath.toFile)) { zipFile =>
      for (entry <- zipFile.entries.asScala) {
        val path = outputPath.resolve(entry.getName)
        if (entry.isDirectory) {
          Files.createDirectories(path)
        } else {
          Files.createDirectories(path.getParent)
          Files.copy(zipFile.getInputStream(entry), path)
        }
      }
    }(???)

  processZip(Paths.get("/Users/a1/Downloads/study/executables/executables.zip"))

}
