package model

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.util.{Failure, Success, Try}

trait JsonFileRepository:

  protected def fileName: String

  protected def loadAllFrom[T](decodeOne: Json.Value => Either[String, T]): Try[List[T]] =
    for
      raw <- readFile()
      items <- eitherToTry(Json.parse(raw).flatMap(v => Json.decodeArray(v)(decodeOne)))
    yield items

  private val dataDir: Path =
    Paths.get(System.getProperty("user.home"), ".subsidy-eligibility-calculator")

  private val dataFile: Path = dataDir.resolve(fileName)

  private def ensureDataDir(): Try[Unit] =
    Try {
      if !Files.exists(dataDir) then Files.createDirectories(dataDir)
      ()
    }

  private def readFile(): Try[String] =
    Try(Files.exists(dataFile)).flatMap { exists =>
      if exists then Try(Files.readString(dataFile, StandardCharsets.UTF_8))
      else Success("[]")
    }

  protected def writeFile(content: String): Try[Unit] =
    for
      _ <- ensureDataDir()
      _ <- Try(
        Files.writeString(
          dataFile,
          content,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
      )
    yield ()

  private def eitherToTry[A](e: Either[String, A]): Try[A] =
    e.fold(message => Failure(new RuntimeException(message)), Success(_))

  protected def jsonString(s: String): String = "\"" + escapeJson(s) + "\""

  private def escapeJson(s: String): String =
    s.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c.toString
    }
