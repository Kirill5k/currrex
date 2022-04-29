package currexx.core.market.db

import currexx.core.market.{IndicatorState, MarketState}
import currexx.domain.market.{CurrencyPair, Indicator, PriceRange, TradeOrder}
import currexx.domain.user.UserId
import io.circe.Codec
import mongo4cats.bson.ObjectId
import mongo4cats.circe.given

import java.time.Instant

final case class MarketStateEntity(
    _id: ObjectId,
    userId: ObjectId,
    currencyPair: CurrencyPair,
    currentPosition: Option[TradeOrder.Position],
    latestPrice: Option[PriceRange],
    signals: Map[Indicator, List[IndicatorState]],
    lastUpdatedAt: Option[Instant]
) derives Codec.AsObject:
  def toDomain: MarketState = MarketState(UserId(userId), currencyPair, currentPosition, latestPrice, signals, lastUpdatedAt)

object MarketStateEntity:
  def make(
      userId: UserId,
      currencyPair: CurrencyPair,
      currentPosition: Option[TradeOrder.Position] = None,
      latestPrice: Option[PriceRange] = None,
      signals: Map[Indicator, List[IndicatorState]] = Map.empty,
      lastUpdatedAt: Option[Instant] = None
  ): MarketStateEntity =
    MarketStateEntity(ObjectId(), userId.toObjectId, currencyPair, currentPosition, latestPrice, signals, lastUpdatedAt)
