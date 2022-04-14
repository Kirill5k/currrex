package currexx.clients.alphavantage

import cats.syntax.either.*
import currexx.clients.alphavantage.AlphaVantageClient.{DailyResponseMetadata, IntradayResponseMetadata, OHLC}
import currexx.domain.errors.AppError
import currexx.domain.market.{MarketTimeSeriesData, PriceRange}
import io.circe.JsonObject

import java.time.{LocalDate, LocalDateTime, ZoneId}
import scala.collection.immutable.ListMap

private[alphavantage] object ResponseMapper {

  def mapDailyTimeSeriesData(res: JsonObject): Either[AppError, List[PriceRange]] =
    for
      metaJson <- res("Meta Data").toRight(AppError.JsonParsingFailure(res.toString, "Missing 'Meta Data' field"))
      meta     <- metaJson.as[DailyResponseMetadata].leftMap(e => AppError.JsonParsingFailure(res.toString, e.getMessage))
      timeSeriesField = "Time Series FX (Daily)"
      pricesJson <- res(timeSeriesField).toRight(AppError.JsonParsingFailure(res.toString, s"Missing '$timeSeriesField' field"))
      prices     <- pricesJson.as[ListMap[String, OHLC]].leftMap(e => AppError.JsonParsingFailure(res.toString, e.getMessage))
    yield prices.zipWithIndex.map { case ((date, priceRange), i) =>
      val dateTime = if (i == 0) meta.`5. Last Refreshed`.replaceFirst(" ", "T") else s"${date}T00:00:00"
      PriceRange(
        priceRange.`1. open`,
        priceRange.`2. high`,
        priceRange.`3. low`,
        priceRange.`4. close`,
        LocalDateTime.parse(dateTime).atZone(ZoneId.of(meta.`6. Time Zone`)).toInstant
      )
    }.toList

  def mapIntradayTimeSeriesData(res: JsonObject): Either[AppError, List[PriceRange]] =
    for
      metaJson <- res("Meta Data").toRight(AppError.JsonParsingFailure(res.toString, "Missing 'Meta Data' field"))
      meta     <- metaJson.as[IntradayResponseMetadata].leftMap(e => AppError.JsonParsingFailure(res.toString, e.getMessage))
      timeSeriesField = s"Time Series FX (${meta.`5. Interval`})"
      pricesJson <- res(timeSeriesField).toRight(AppError.JsonParsingFailure(res.toString, s"Missing '$timeSeriesField' field"))
      prices     <- pricesJson.as[ListMap[String, OHLC]].leftMap(e => AppError.JsonParsingFailure(res.toString, e.getMessage))
    yield prices.zipWithIndex.map { case ((date, priceRange), i) =>
      val dateTime = if (i == 0) meta.`4. Last Refreshed`.replaceFirst(" ", "T") else date.replaceFirst(" ", "T")
      PriceRange(
        priceRange.`1. open`,
        priceRange.`2. high`,
        priceRange.`3. low`,
        priceRange.`4. close`,
        LocalDateTime.parse(dateTime).atZone(ZoneId.of(meta.`7. Time Zone`)).toInstant
      )
    }.toList
}
