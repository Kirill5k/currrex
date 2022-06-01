package currexx.domain.market

import org.latestbit.circe.adt.codec.*

enum Condition derives JsonTaggedAdt.EncoderWithConfig, JsonTaggedAdt.DecoderWithConfig:
  case CrossingUp
  case CrossingDown
  case AboveThreshold(threshold: BigDecimal, value: BigDecimal)
  case BelowThreshold(threshold: BigDecimal, value: BigDecimal)
  case TrendDirectionChange(from: Trend, to: Trend)

object Condition {
  given JsonTaggedAdt.Config[Condition] = JsonTaggedAdt.Config.Values[Condition](
    mappings = Map(
      "crossing-up"            -> JsonTaggedAdt.tagged[Condition.CrossingUp.type],
      "crossing-down"          -> JsonTaggedAdt.tagged[Condition.CrossingDown.type],
      "above-threshold"        -> JsonTaggedAdt.tagged[Condition.AboveThreshold],
      "below-threshold"        -> JsonTaggedAdt.tagged[Condition.BelowThreshold],
      "trend-direction-change" -> JsonTaggedAdt.tagged[Condition.TrendDirectionChange]
    ),
    strict = true,
    typeFieldName = "kind"
  )

  def lineCrossing(line1Curr: BigDecimal, line2Curr: BigDecimal, line1Prev: BigDecimal, line2Prev: BigDecimal): Option[Condition] =
    (line1Curr, line2Curr, line1Prev, line2Prev) match
      case (l1c, l2c, l1p, l2p) if l1c > l2c && l1p < l2p => Some(Condition.CrossingUp)
      case (l1c, l2c, l1p, l2p) if l1c < l2c && l1p > l2p => Some(Condition.CrossingDown)
      case _                                              => None
}
