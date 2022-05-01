package currexx.domain.market

import io.circe.syntax.*
import io.circe.{Codec, CursorOp, Decoder, DecodingFailure, Encoder, Json}

import scala.util.Try

sealed trait TradeOrder(val kind: String)

object TradeOrder {
  enum Position:
    case Buy, Sell

  object Position:
    inline given Encoder[Position] = Encoder.encodeString.contramap(_.toString.toLowerCase)
    inline given Decoder[Position] = Decoder.decodeString.emap { p =>
      Try(Position.valueOf(p.toLowerCase.capitalize)).toOption
        .toRight(s"$p is not valid position kind. Accepted values: ${Position.values.map(_.toString.toLowerCase).mkString(", ")}")
    }

  final case class Enter(
      position: Position,
      volume: BigDecimal,
      stopLoss: Option[BigDecimal],
      trailingStopLoss: Option[BigDecimal],
      takeProfit: Option[BigDecimal]
  ) extends TradeOrder("enter")
      derives Codec.AsObject

  case object Exit extends TradeOrder("exit")

  private val discriminatorField: String                 = "kind"
  private def discriminatorJson(order: TradeOrder): Json = Map(discriminatorField -> order.kind).asJson

  inline given Decoder[TradeOrder] = Decoder.instance { c =>
    c.downField(discriminatorField).as[String].flatMap {
      case "enter" => c.as[Enter]
      case "exit"  => Right(Exit)
      case kind    => Left(DecodingFailure(s"Unexpected order kind $kind", List(CursorOp.Field(discriminatorField))))
    }
  }
  inline given Encoder[TradeOrder] = Encoder.instance {
    case enter: Enter => enter.asJson.deepMerge(discriminatorJson(enter))
    case exit @ Exit  => discriminatorJson(exit)
  }
}
