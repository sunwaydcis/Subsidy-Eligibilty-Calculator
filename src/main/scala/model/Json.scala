package model

import scala.util.Try

object Json:

  sealed trait Value
  final case class JObj(fields: Map[String, Value]) extends Value // JSON Object ({...})
  private final case class JArr(items: List[Value]) extends Value         // JSON Array ([...])
  private final case class JStr(value: String) extends Value              // JSON String ("...")
  private final case class JNum(value: BigDecimal) extends Value          // JSON Number (42, 3.14, -7)
  private final case class JBool(value: Boolean) extends Value            // JSON Boolean (true/false)
  private case object JNull extends Value                                 // JSON null
  
  def parse(input: String): Either[String, Value] =
    for
      parsed <- parseValue(input, skipWs(input, 0))
      (value, afterValue) = parsed
      end = skipWs(input, afterValue)
      result <- Either.cond(end == input.length, value, s"Unexpected trailing content at position $end")
    yield result

  // skips whitespace
  private def skipWs(s: String, i: Int): Int =
    if i < s.length && s.charAt(i).isWhitespace then skipWs(s, i + 1) else i

  private def parseValue(s: String, i: Int): Either[String, (Value, Int)] =
    if i >= s.length then Left(s"Unexpected end of input at position $i")
    else
      s.charAt(i) match
        case '{' => parseObject(s, i + 1)
        case '[' => parseArray(s, i + 1)
        case '"' => parseString(s, i + 1).map { case (str, next) => (JStr(str), next) }
        case 't' => parseLiteral(s, i, "true", JBool(true))
        case 'f' => parseLiteral(s, i, "false", JBool(false))
        case 'n' => parseLiteral(s, i, "null", JNull)
        case c if c == '-' || c.isDigit => parseNumber(s, i)
        case other => Left(s"Unexpected character '$other' at position $i")

  private def parseLiteral(s: String, i: Int, literal: String, value: Value): Either[String, (Value, Int)] =
    if s.startsWith(literal, i) then Right((value, i + literal.length))
    else Left(s"Expected '$literal' at position $i")

  private def parseString(s: String, i: Int): Either[String, (String, Int)] =
    def loop(idx: Int, acc: String): Either[String, (String, Int)] =
      if idx >= s.length then Left(s"Unterminated string starting near position $i")
      else
        s.charAt(idx) match
          case '"' => Right((acc, idx + 1))
          case '\\' =>
            if idx + 1 >= s.length then Left(s"Dangling escape at position $idx")
            else
              s.charAt(idx + 1) match
                case '"'  => loop(idx + 2, acc + '"')
                case '\\' => loop(idx + 2, acc + '\\')
                case '/'  => loop(idx + 2, acc + '/')
                case 'n'  => loop(idx + 2, acc + '\n')
                case 't'  => loop(idx + 2, acc + '\t')
                case 'r'  => loop(idx + 2, acc + '\r')
                case 'b'  => loop(idx + 2, acc + '\b')
                case 'f'  => loop(idx + 2, acc + '\f')
                case 'u' =>
                  if idx + 6 > s.length then Left(s"Invalid unicode escape at position $idx")
                  else
                    val hex = s.substring(idx + 2, idx + 6)
                    Try(Integer.parseInt(hex, 16)).toOption match
                      case Some(code) => loop(idx + 6, acc + code.toChar)
                      case None       => Left(s"Invalid unicode escape '\\u$hex' at position $idx")
                case other => Left(s"Unknown escape '\\$other' at position $idx")
          case c => loop(idx + 1, acc + c)
    loop(i, "")

  private def parseNumber(s: String, i: Int): Either[String, (Value, Int)] =
    def isNumberChar(c: Char) = c.isDigit || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
    def loop(idx: Int): Int = if idx < s.length && isNumberChar(s.charAt(idx)) then loop(idx + 1) else idx
    val end = loop(i)
    val token = s.substring(i, end)
    Try(BigDecimal(token)).toOption match
      case Some(n) => Right((JNum(n), end))
      case None    => Left(s"Invalid number '$token' at position $i")

  private def parseArray(s: String, i: Int): Either[String, (Value, Int)] =
    val afterOpenWs = skipWs(s, i)
    if afterOpenWs < s.length && s.charAt(afterOpenWs) == ']' then Right((JArr(Nil), afterOpenWs + 1))
    else
      def loop(idx: Int, acc: List[Value]): Either[String, (List[Value], Int)] =
        for
          parsedItem <- parseValue(s, skipWs(s, idx))
          (value, afterValue) = parsedItem
          afterValueWs = skipWs(s, afterValue)
          result <-
            if afterValueWs < s.length && s.charAt(afterValueWs) == ',' then loop(afterValueWs + 1, value :: acc)
            else if afterValueWs < s.length && s.charAt(afterValueWs) == ']' then
              Right(((value :: acc).reverse, afterValueWs + 1))
            else Left(s"Expected ',' or ']' at position $afterValueWs")
        yield result
      loop(afterOpenWs, Nil).map { case (items, next) => (JArr(items), next) }

  private def parseObject(s: String, i: Int): Either[String, (Value, Int)] =
    val afterOpenWs = skipWs(s, i)
    if afterOpenWs < s.length && s.charAt(afterOpenWs) == '}' then Right((JObj(Map.empty), afterOpenWs + 1))
    else
      def loop(idx: Int, acc: Map[String, Value]): Either[String, (Map[String, Value], Int)] =
        val keyStart = skipWs(s, idx)
        for
          _                <- Either.cond(keyStart < s.length && s.charAt(keyStart) == '"', (), s"Expected object key at position $keyStart")
          parsedKey        <- parseString(s, keyStart + 1)
          (key, afterKey)   = parsedKey
          colonPos          = skipWs(s, afterKey)
          _                <- Either.cond(colonPos < s.length && s.charAt(colonPos) == ':', (), s"Expected ':' at position $colonPos")
          parsedValue      <- parseValue(s, skipWs(s, colonPos + 1))
          (value, afterValue) = parsedValue
          afterValueWs      = skipWs(s, afterValue)
          result <-
            if afterValueWs < s.length && s.charAt(afterValueWs) == ',' then loop(afterValueWs + 1, acc + (key -> value))
            else if afterValueWs < s.length && s.charAt(afterValueWs) == '}' then
              Right((acc + (key -> value), afterValueWs + 1))
            else Left(s"Expected ',' or '}' at position $afterValueWs")
        yield result
      loop(afterOpenWs, Map.empty).map { case (fields, next) => (JObj(fields), next) }

  // ---------------------------------------------------------------
  // Generic decode helpers, reusable by any repository that persists
  // a `List[T]` as a top-level JSON array of objects (which is every
  // repository in this project so far). Pulling these out here means
  // `HouseholdRepository` and `EligibilityHistoryRepository` each only
  // write the *shape-specific* half of decoding (which fields a
  // Household/EligibilityCheckRecord has), not the generic
  // "extract a String/array from a Value" plumbing.
  // ---------------------------------------------------------------

  def asString(v: Value): Either[String, String] = v match
    case JStr(s) => Right(s)
    case other   => Left(s"Expected a string, got: $other")

  def asNumber(v: Value): Either[String, BigDecimal] = v match
    case JNum(n) => Right(n)
    case other   => Left(s"Expected a number, got: $other")

  def asArray(v: Value): Either[String, List[Value]] = v match
    case JArr(items) => Right(items)
    case other        => Left(s"Expected an array, got: $other")
  
  def field(fields: Map[String, Value], name: String): Either[String, Value] =
    fields.get(name).toRight(s"Missing field '$name'")
  
  def traverse[A, B](items: List[A])(f: A => Either[String, B]): Either[String, List[B]] =
    items.foldRight[Either[String, List[B]]](Right(Nil)) { (item, accE) =>
      for
        b   <- f(item)
        acc <- accE
      yield b :: acc
    }

  def decodeArray[T](v: Value)(decodeOne: Value => Either[String, T]): Either[String, List[T]] = v match
    case JArr(items) => traverse(items)(decodeOne)
    case other        => Left(s"Expected a JSON array, got: $other")
