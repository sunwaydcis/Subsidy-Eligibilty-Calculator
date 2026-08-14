package model

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

object ChecklistExporter:

  private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  
  def exportFile(
                  destination: File,
                  householdId: String,
                  programName: String,
                  documents: List[(String, Boolean)]
                ): Try[Unit] =
    Try {
      Files.writeString(
        destination.toPath,
        formatChecklist(householdId, programName, documents),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      ()
    }

  private def formatChecklist(
                               householdId: String,
                               programName: String,
                               documents: List[(String, Boolean)]
                             ): String =
    val header = List(
      s"Checklist: $programName",
      s"Household: $householdId",
      s"Generated: ${LocalDateTime.now().format(timestampFormat)}",
      ""
    )
    val documentLines = documents.map { case (document, ticked) =>
      val marker = if ticked then "[x]" else "[ ]"
      s"$marker $document"
    }
    val tickedCount = documents.count(_._2)
    val footer = List("", s"$tickedCount of ${documents.size} document(s) gathered.")

    (header ++ documentLines ++ footer).mkString(System.lineSeparator())
