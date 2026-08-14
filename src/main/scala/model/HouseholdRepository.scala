package model

import scala.util.Try

object HouseholdRepository extends JsonFileRepository:

  protected def fileName: String = "households.json"

  // ---------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------

  def loadAll(): Try[List[Household]] = loadAllFrom(decodeHousehold)

  def list(): Try[List[Household]] = loadAll()
  
  def save(household: Household): Try[Unit] =
    for
      existing <- loadAll()
      updated = existing.filterNot(_.id == household.id) :+ household
      _ <- writeFile(toJson(updated))
    yield ()

  def saveAll(households: List[Household]): Try[Unit] =
    writeFile(toJson(households))

  // ---------------------------------------------------------------
  // JSON encoding — pure functions, can't fail
  // ---------------------------------------------------------------

  private def toJson(households: List[Household]): String =
    households.map(encodeHousehold).mkString("[", ",", "]")

  private def encodeHousehold(h: Household): String =
    val dependentsJson = h.dependents.map(encodeDependent).mkString("[", ",", "]")
    val assetsJson = h.assets.map(encodeAsset).mkString("[", ",", "]")
    s"""{"id":${jsonString(h.id)},"monthlyIncome":${h.monthlyIncome},"dependents":$dependentsJson,"assets":$assetsJson}"""

  private def encodeDependent(d: Dependent): String =
    s"""{"name":${jsonString(d.name)},"age":${d.age},"relationship":${jsonString(d.relationship)}}"""

  private def encodeAsset(a: Asset): String =
    s"""{"description":${jsonString(a.description)},"estimatedValue":${a.estimatedValue}}"""

  // ---------------------------------------------------------------
  // JSON decoding — Either-based; no exception ever escapes this section
  // ---------------------------------------------------------------

  private def decodeHousehold(v: Json.Value): Either[String, Household] = v match
    case Json.JObj(fields) =>
      for
        id          <- Json.field(fields, "id").flatMap(Json.asString)
        income      <- Json.field(fields, "monthlyIncome").flatMap(Json.asNumber)
        dependentsV <- Json.field(fields, "dependents").flatMap(Json.asArray)
        assetsV     <- Json.field(fields, "assets").flatMap(Json.asArray)
        dependents  <- Json.traverse(dependentsV)(decodeDependent)
        assets      <- Json.traverse(assetsV)(decodeAsset)
      yield Household(id, income, dependents, assets)
    case other => Left(s"Expected a household object, got: $other")

  private def decodeDependent(v: Json.Value): Either[String, Dependent] = v match
    case Json.JObj(fields) =>
      for
        name         <- Json.field(fields, "name").flatMap(Json.asString)
        ageNum       <- Json.field(fields, "age").flatMap(Json.asNumber)
        relationship <- Json.field(fields, "relationship").flatMap(Json.asString)
      yield Dependent(name, ageNum.toInt, relationship)
    case other => Left(s"Expected a dependent object, got: $other")

  private def decodeAsset(v: Json.Value): Either[String, Asset] = v match
    case Json.JObj(fields) =>
      for
        description <- Json.field(fields, "description").flatMap(Json.asString)
        value       <- Json.field(fields, "estimatedValue").flatMap(Json.asNumber)
      yield Asset(description, value)
    case other => Left(s"Expected an asset object, got: $other")
