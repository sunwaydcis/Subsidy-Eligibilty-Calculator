package model

import java.time.LocalDateTime
import scala.util.Try

object EligibilityHistoryRepository extends JsonFileRepository:

  protected def fileName: String = "eligibility-history.json"

  def loadAll(): Try[List[EligibilityCheckRecord]] = loadAllFrom(decodeRecord)

  def list(): Try[List[EligibilityCheckRecord]] = loadAll()
  
  def recordCheck(record: EligibilityCheckRecord): Try[Unit] =
    for
      existing <- loadAll()
      updated = existing :+ record
      _ <- writeFile(toJson(updated))
    yield ()

  // ---------------------------------------------------------------
  // JSON encoding
  // ---------------------------------------------------------------

  private def toJson(records: List[EligibilityCheckRecord]): String =
    records.map(encodeRecord).mkString("[", ",", "]")

  private def encodeRecord(r: EligibilityCheckRecord): String =
    val programmesJson = r.eligiblePrograms.map(jsonString).mkString("[", ",", "]")
    s"""{"householdId":${jsonString(r.householdId)},"checkedAt":${jsonString(r.checkedAt.toString)},"eligiblePrograms":$programmesJson}"""

  // ---------------------------------------------------------------
  // JSON decoding — Either-based; no exception ever escapes this section
  // ---------------------------------------------------------------

  private def decodeRecord(v: Json.Value): Either[String, EligibilityCheckRecord] = v match
    case Json.JObj(fields) =>
      for
        householdId  <- Json.field(fields, "householdId").flatMap(Json.asString)
        checkedAtRaw <- Json.field(fields, "checkedAt").flatMap(Json.asString)
        checkedAt    <- parseDateTime(checkedAtRaw)
        programmesV  <- Json.field(fields, "eligiblePrograms").flatMap(Json.asArray)
        programmes   <- Json.traverse(programmesV)(Json.asString)
      yield EligibilityCheckRecord(householdId, checkedAt, programmes)
    case other => Left(s"Expected an eligibility check record object, got: $other")

  private def parseDateTime(raw: String): Either[String, LocalDateTime] =
    Try(LocalDateTime.parse(raw)).toOption.toRight(s"Invalid date/time '$raw'")
