package debug

import better.files.{File, StringInterpolations}
import org.apache.commons.io.{FileUtils, FilenameUtils}

import java.io.{FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipInputStream}

object ForTesting extends App {

  def processZip(path: Path) = {

    val zipFile = File(path)

    val name = FilenameUtils.removeExtension(path.getFileName.toString)

    val temp = Files.createTempDirectory(zipFile.path.getParent, ".")

    zipFile.unzipTo(destination = temp)

    println(temp.resolve(name))

    temp
      .resolve(name)
      .toFile
      .listFiles()
      .foreach(f => println(s"$f ${Files.isExecutable(Paths.get(f.getAbsolutePath))}"))

  }

  processZip(Paths.get("/Users/a1/Downloads/study/executables.zip"))

}
