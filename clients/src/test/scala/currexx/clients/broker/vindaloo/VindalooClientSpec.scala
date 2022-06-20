package currexx.clients.broker.vindaloo

import cats.effect.IO
import currexx.clients.ApiClientSpec
import currexx.domain.market.{CurrencyPair, Interval, TradeOrder}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import squants.market.{GBP, USD}
import sttp.client3.{Response, SttpBackend}

class VindalooClientSpec extends ApiClientSpec {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config = VindalooConfig("http://vindaloo.com")
  val pair   = CurrencyPair(GBP, USD)

  "A VindalooClient" should {
    "send enter market requests" in {
      val testingBackend: SttpBackend[IO, Any] = backendStub
        .whenRequestMatchesPartial {
          case r if r.isPost && r.isGoingTo("vindaloo.com/15/25/0/0/buy/GBPUSD/0.1") =>
            Response.ok("ok")
          case _ => throw new RuntimeException()
        }

      val result = for
        client <- VindalooClient.make[IO](config, testingBackend)
        res    <- client.submit("15", pair, TradeOrder.Enter(TradeOrder.Position.Buy, BigDecimal(0.1), Some(BigDecimal(25)), None, None))
      yield res

      result.asserting(_ mustBe ())
    }

    "send exit market requests" in {
      val testingBackend: SttpBackend[IO, Any] = backendStub
        .whenRequestMatchesPartial {
          case r if r.isPost && r.isGoingTo("vindaloo.com/close/15/GBPUSD") =>
            Response.ok("ok")
          case _ => throw new RuntimeException()
        }

      val result = for
        client <- VindalooClient.make[IO](config, testingBackend)
        res    <- client.submit("15", pair, TradeOrder.Exit)
      yield res

      result.asserting(_ mustBe ())
    }
  }
}
