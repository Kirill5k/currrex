package currexx.clients.broker.xtb

import cats.effect.IO
import currexx.clients.ApiClientSpec
import currexx.clients.broker.BrokerParameters
import currexx.clients.broker.vindaloo.{VindalooClient, VindalooConfig}
import currexx.domain.errors.AppError
import currexx.domain.market.{CurrencyPair, TradeOrder}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import squants.market.{GBP, USD}
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.{Response, SttpBackend, SttpBackendOptions}
import sttp.model.StatusCode
import sttp.ws.{WebSocket, WebSocketFrame}
import sttp.ws.testing.WebSocketStub

import scala.concurrent.duration.*

class XtbClientSpec extends ApiClientSpec {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config = XtbConfig("wss://ws.xtb.com")
  val pair   = CurrencyPair(GBP, USD)

  "A XtbClient" should {
    "return error on failed authentication" ignore {
      val testingBackend: SttpBackend[IO, Fs2Streams[IO] with WebSockets] = backendStub
        .whenRequestMatches(_ => true)
        .thenRespond(
          WebSocketStub.noInitialReceive
            .thenRespond(_ => List(WebSocketFrame.text(json("xtb/login-failure-response.json"))))
        )

      val result = for
        client <- XtbClient.make[IO](config, testingBackend)
        res    <- client.submit(BrokerParameters.Xtb("foo", "bar", true), pair, TradeOrder.Exit)
      yield res

      result.assertError(AppError.AccessDenied("foo"))
    }

    "send enter market request" ignore {
      val result = AsyncHttpClientFs2Backend
        .resource[IO](SttpBackendOptions(connectionTimeout = 3.minutes, proxy = None))
        .use { backend =>
          for
            client <- XtbClient.make[IO](config, backend)
            order = TradeOrder.Enter(TradeOrder.Position.Buy, BigDecimal(0.1))
            res <- client.submit(BrokerParameters.Xtb("13529575", "Boroda123", true), pair, order)
          yield res
        }

      result.asserting(_ mustBe ())
    }

    "send exit market request" in {
      val result = AsyncHttpClientFs2Backend
        .resource[IO](SttpBackendOptions(connectionTimeout = 3.minutes, proxy = None))
        .use { backend =>
          for
            client <- XtbClient.make[IO](config, backend)
            res    <- client.submit(BrokerParameters.Xtb("13529575", "Boroda123", true), pair, TradeOrder.Exit)
          yield res
        }

      result.asserting(_ mustBe ())
    }
  }
}
