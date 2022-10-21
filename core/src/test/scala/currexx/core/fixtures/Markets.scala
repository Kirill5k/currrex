package currexx.core.fixtures

import cats.data.NonEmptyList
import currexx.clients.broker.BrokerParameters
import currexx.core.market.{IndicatorState, MarketState, PositionState}
import currexx.domain.market.Currency.{EUR, GBP, USD}
import currexx.domain.market.{Condition, CurrencyPair, Indicator, IndicatorKind, Interval, MarketTimeSeriesData, PriceRange, TradeOrder, Trend, ValueSource, ValueTransformation}

import java.time.Instant

object Markets {
  lazy val trendChangeDetection: Indicator =
    Indicator.TrendChangeDetection(ValueSource.Close, ValueTransformation.HMA(16))
  lazy val thresholdCrossing: Indicator =
    Indicator.ThresholdCrossing(ValueSource.Close, ValueTransformation.STOCH(14), 80d, 20d)

  lazy val gbpeur: CurrencyPair = CurrencyPair(GBP, EUR)
  lazy val gbpusd: CurrencyPair = CurrencyPair(GBP, USD)

  lazy val ts: Instant            = Instant.now
  lazy val priceRange: PriceRange = PriceRange(BigDecimal(2.0), BigDecimal(4.0), BigDecimal(1.0), BigDecimal(3.0), BigDecimal(1000), ts)
  lazy val timeSeriesData: MarketTimeSeriesData = MarketTimeSeriesData(gbpeur, Interval.H1, NonEmptyList.one(priceRange))

  lazy val positionState: PositionState = PositionState(TradeOrder.Position.Buy, ts, priceRange.close)

  lazy val indicatorState: IndicatorState =
    IndicatorState(Signals.trendDirectionChanged.condition, Signals.trendDirectionChanged.time, trendChangeDetection)
  lazy val indicatorStates: Map[IndicatorKind, List[IndicatorState]] = Map(trendChangeDetection.kind -> List(indicatorState))

  lazy val state: MarketState = MarketState(
    Users.uid,
    gbpeur,
    Some(positionState),
    Map.empty,
    Some(ts),
    Some(ts)
  )

  lazy val stateWithSignal: MarketState = state.copy(signals = Map(trendChangeDetection.kind -> List(indicatorState)))

  lazy val priceRanges: NonEmptyList[PriceRange] = NonEmptyList
    .of(
      ("1.26205", "1.26329", "1.25170", "1.25200"),
      ("1.24950", "1.26382", "1.24500", "1.26329"), // 04.05.2022
      ("1.24860", "1.25673", "1.24680", "1.24937"),
      ("1.25780", "1.25971", "1.24710", "1.24879"),
      ("1.24558", "1.26143", "1.24500", "1.25710"),
      ("1.25394", "1.25698", "1.24090", "1.24554"),
      ("1.25713", "1.26017", "1.25000", "1.25396"),
      ("1.27420", "1.27723", "1.25680", "1.25711"),
      ("1.28356", "1.28392", "1.26950", "1.27420"),
      ("1.30328", "1.30350", "1.28200", "1.28370"),
      ("1.30620", "1.30902", "1.30210", "1.30320"),
      ("1.29977", "1.30705", "1.29940", "1.30615"),
      ("1.30083", "1.30407", "1.29790", "1.29980"),
      ("1.30551", "1.30645", "1.30030", "1.30078"),
      ("1.30730", "1.30780", "1.30433", "1.30580"),
      ("1.31154", "1.31472", "1.30320", "1.30707"),
      ("1.30017", "1.31179", "1.29710", "1.31147"),
      ("1.30283", "1.30537", "1.29920", "1.30010"),
      ("1.30314", "1.30566", "1.29870", "1.30294"),
      ("1.30735", "1.30771", "1.29800", "1.30310"),
      ("1.30637", "1.31065", "1.30500", "1.30740"),
      ("1.30728", "1.31080", "1.30440", "1.30650"),
      ("1.31151", "1.31667", "1.30650", "1.30712"),
      ("1.31064", "1.31370", "1.30910", "1.31140"),
      ("1.31352", "1.31515", "1.30850", "1.31120"),
      ("1.31329", "1.31758", "1.31040", "1.31371"),
      ("1.30960", "1.31825", "1.30870", "1.31332"),
      ("1.30919", "1.31600", "1.30490", "1.30970"),
      ("1.31796", "1.31849", "1.30656", "1.30921"),
      ("1.31817", "1.32247", "1.31580", "1.31880"),
      ("1.32021", "1.32141", "1.31550", "1.31830"),
      ("1.32575", "1.32985", "1.31740", "1.32025"),
      ("1.31644", "1.32737", "1.31170", "1.32583"),
      ("1.31708", "1.32102", "1.31240", "1.31622"),
      ("1.31483", "1.31970", "1.31070", "1.31800"),
      ("1.31433", "1.32110", "1.30850", "1.31455"),
      ("1.30370", "1.31564", "1.30330", "1.31420"),
      ("1.30030", "1.30887", "1.29970", "1.30355"),
      ("1.30346", "1.30790", "1.29980", "1.30012"),
      ("1.30839", "1.31250", "1.30250", "1.30343"),
      ("1.31798", "1.31947", "1.30780", "1.30850"),
      ("1.30992", "1.31898", "1.30850", "1.31757"),
      ("1.31044", "1.31446", "1.30800", "1.31012"),
      ("1.32258", "1.32406", "1.30990", "1.31033"),
      ("1.33470", "1.33545", "1.31990", "1.32287"),
      ("1.34062", "1.34180", "1.33150", "1.33448"),
      ("1.33205", "1.34077", "1.32700", "1.34052"),
      ("1.34137", "1.34371", "1.33000", "1.33250"),
      ("1.33118", "1.34315", "1.33118", "1.34161"),
      ("1.33721", "1.34388", "1.33650", "1.34053"),
      ("1.35408", "1.35494", "1.32710", "1.33749"),
      ("1.35802", "1.36206", "1.35330", "1.35430"),
      ("1.36000", "1.36046", "1.35360", "1.35801"),
      ("1.35926", "1.36385", "1.35830", "1.35973"),
      ("1.36142", "1.36427", "1.35710", "1.35856"),
      ("1.35823", "1.36381", "1.35530", "1.36130"),
      ("1.35331", "1.36010", "1.35249", "1.35860"),
      ("1.35229", "1.35669", "1.34850", "1.35341"),
      ("1.35506", "1.35720", "1.34930", "1.35233"),
      ("1.35538", "1.36095", "1.35110", "1.35584"),
      ("1.35340", "1.36435", "1.35210", "1.35560"),
      ("1.35438", "1.35893", "1.35250", "1.35340"),
      ("1.35351", "1.35640", "1.35050", "1.35420"),
      ("1.35321", "1.35506", "1.34890", "1.35357"),
      ("1.35929", "1.36151", "1.35020", "1.35269"),
      ("1.35780", "1.36279", "1.35350", "1.35947"),
      ("1.35198", "1.35875", "1.35122", "1.35757"),
      ("1.34429", "1.35282", "1.34310", "1.35205"),
      ("1.33937", "1.34605", "1.33850", "1.34471"),
      ("1.33816", "1.34327", "1.33630", "1.33980"),
      ("1.34589", "1.34679", "1.33560", "1.33819"),
      ("1.35030", "1.35245", "1.34420", "1.34622"),
      ("1.34860", "1.35187", "1.34340", "1.35015"),
      ("1.35512", "1.35653", "1.34380", "1.34870"),
      ("1.35980", "1.36023", "1.35440", "1.35504"),
      ("1.36115", "1.36617", "1.35840", "1.35992"),
      ("1.35910", "1.36489", "1.35850", "1.36120"),
      ("1.36441", "1.36615", "1.35710", "1.35960"),
      ("1.36687", "1.36898", "1.36350", "1.36460"),
      ("1.37020", "1.37429", "1.36510", "1.36762"),
      ("1.36971", "1.37488", "1.36968", "1.37050"),
      ("1.36358", "1.37144", "1.36190", "1.37000"),
      ("1.35726", "1.36363", "1.35590", "1.36357"),
      ("1.35819", "1.36036", "1.35300", "1.35740"),
      ("1.35284", "1.35974", "1.35240", "1.35860"),
      ("1.35517", "1.35590", "1.34870", "1.35290"),
      ("1.35261", "1.35985", "1.35197", "1.35541"),
      ("1.34749", "1.35573", "1.34580", "1.35290"),
      ("1.35290", "1.35353", "1.34290", "1.34790"),
      ("1.34971", "1.35505", "1.34630", "1.35177"),
      ("1.34869", "1.35219", "1.34520", "1.34980"),
      ("1.34299", "1.34997", "1.34060", "1.34890"),
      ("1.34386", "1.34617", "1.34130", "1.34340"),
      ("1.33900", "1.34451", "1.33871", "1.34390"),
      ("1.34060", "1.34210", "1.33835", "1.33849"),
      ("1.33448", "1.34378", "1.33400", "1.34055"),
      ("1.32630", "1.33634", "1.32380", "1.33481"),
      ("1.32052", "1.32710", "1.31950", "1.32630"),
      ("1.32390", "1.32456", "1.31710", "1.32060"),
      ("1.33195", "1.33393", "1.32274", "1.32380")
    )
    .zipWithIndex
    .map { case ((o, h, l, c), index) =>
      PriceRange(BigDecimal(o), BigDecimal(h), BigDecimal(l), BigDecimal(c), BigDecimal(1000), ts.minusSeconds(index.toLong * 86400L))
    }
}
