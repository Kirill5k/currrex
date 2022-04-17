package currexx.domain.market

import io.circe.{Codec, CursorOp, Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax.*

sealed trait Condition(val kind: String)
object Condition {
  final case class CrossingUp()                                 extends Condition("crossing-up") derives Codec.AsObject
  final case class CrossingDown()                               extends Condition("crossing-down") derives Codec.AsObject
  final case class TrendDirectionChange(from: Trend, to: Trend) extends Condition("trend-direction-change") derives Codec.AsObject

  private val discriminatorField: String               = "kind"
  private def discriminatorJson(cond: Condition): Json = Map(discriminatorField -> cond.kind).asJson

  inline given Decoder[Condition] = Decoder.instance { c =>
    c.downField(discriminatorField).as[String].flatMap {
      case "crossing-up"            => c.as[CrossingUp]
      case "crossing-down"          => c.as[CrossingDown]
      case "trend-direction-change" => c.as[TrendDirectionChange]
      case kind                     => Left(DecodingFailure(s"Unexpected condition kind $kind", List(CursorOp.Field(discriminatorField))))
    }
  }
  inline given Encoder[Condition] = Encoder.instance {
    case crossUp: CrossingUp                        => crossUp.asJson.deepMerge(discriminatorJson(crossUp))
    case crossDown: CrossingDown                    => crossDown.asJson.deepMerge(discriminatorJson(crossDown))
    case trendDirectionChange: TrendDirectionChange => trendDirectionChange.asJson.deepMerge(discriminatorJson(trendDirectionChange))
  }

  def lineCrossing(line1Curr: BigDecimal, line2Curr: BigDecimal, line1Prev: BigDecimal, line2Prev: BigDecimal): Option[Condition] =
    (line1Curr, line2Curr, line1Prev, line2Prev) match
      case (l1c, l2c, l1p, l2p) if l1c > l2c && l1p < l2p => Some(Condition.CrossingUp())
      case (l1c, l2c, l1p, l2p) if l1c < l2c && l1p > l2p => Some(Condition.CrossingDown())
      case _                                              => None
}
