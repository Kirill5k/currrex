package currexx.clients.broker.xtb

import cats.Monad
import cats.effect.{Async, Ref}
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import currexx.clients.HttpClient
import currexx.clients.broker.BrokerParameters
import currexx.clients.broker.xtb.XtbResponse.TickPricesData
import currexx.domain.errors.AppError
import currexx.domain.market.{CurrencyPair, TradeOrder}
import io.circe.syntax.*
import org.typelevel.log4cats.Logger
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.ws.WebSocketFrame
import sttp.client3.*
import sttp.model.Uri
import fs2.{Pipe, Stream}

import scala.concurrent.duration.*
import java.nio.charset.StandardCharsets

private[clients] trait XtbClient[F[_]] extends HttpClient[F]:
  def submit(params: BrokerParameters.Xtb, pair: CurrencyPair, order: TradeOrder): F[Unit]

final private class LiveXtbClient[F[_]](
    private val config: XtbConfig,
    override protected val backend: SttpBackend[F, Fs2Streams[F] with WebSockets]
)(using
    F: Async[F],
    logger: Logger[F]
) extends XtbClient[F] {

  override protected val name: String                                   = "xtb"
  override protected val delayBetweenConnectionFailures: FiniteDuration = 5.seconds

  override def submit(params: BrokerParameters.Xtb, pair: CurrencyPair, order: TradeOrder): F[Unit] =
    basicRequest
      .response(asWebSocketStream(Fs2Streams[F])(orderPlacementPipe(params, pair, order)))
      .get(uri"${config.baseUri}/${if (params.demo) "demo" else "real"}")
      .send(backend)
      .void

  private def orderPlacementPipe(
      params: BrokerParameters.Xtb,
      pair: CurrencyPair,
      order: TradeOrder
  ): Pipe[F, WebSocketFrame.Data[_], WebSocketFrame] = { input =>
    Stream.eval(Ref.of(XtbClient.WsState(None, None))).flatMap { state =>
      Stream.emit(WebSocketFrame.text(XtbRequest.login(params.userId, params.password).asJson.noSpaces)) ++
        input
          .evalTap(m => F.delay(println(s"received $m")))
          .map {
            case WebSocketFrame.Text(jsonPayload, _, _) => XtbResponse.fromJson(jsonPayload)
            case WebSocketFrame.Binary(bytes, _, _)     => XtbResponse.fromJson(new String(bytes, StandardCharsets.UTF_8))
            case _ | null                               => Right(XtbResponse.Void)
          }
          .rethrow
          .flatMap {
            case XtbResponse.Login(sessionId) =>
              Stream.eval(state.update(_.withSessionId(sessionId))).drain ++
                Stream.emit(WebSocketFrame.text(XtbRequest.tickPrice(sessionId, pair).asJson.noSpaces))
            case XtbResponse.TickPrices(tickPricesData) =>
              for
                sessionId <- Stream.eval(state.get.map(_.sessionId.toRight(AppError.ClientFailure(name, "no session id")))).rethrow
                price     <- Stream(tickPricesData.findAskPriceFor(pair).toRight(AppError.ClientFailure(name, "missing price"))).rethrow
                _         <- Stream.eval(state.update(_.withPrice(price)))
              yield WebSocketFrame.text(XtbRequest.trade(sessionId, pair, order, price).asJson.noSpaces)
            case XtbResponse.OrderPlacement(_) =>
              Stream.emit(WebSocketFrame.close)
            case XtbResponse.Error("BE005", desc) =>
              Stream.emit(WebSocketFrame.close) ++
                Stream.logError(s"$name-client/forbidden: $desc") ++
                Stream.raiseError(AppError.AccessDenied(s"Failed to authenticate with $name: $desc"))
            case XtbResponse.Error(code, desc) =>
              Stream.emit(WebSocketFrame.close) ++
                Stream.logError(s"$name-client/error: $desc") ++
                Stream.raiseError(AppError.ClientFailure(name, s"$code - $desc"))
            case _ =>
              Stream.empty
          }
    }
  }
}

object XtbClient:
  final case class WsState(sessionId: Option[String], price: Option[BigDecimal]):
    def withSessionId(sessionId: String): WsState = copy(sessionId = Some(sessionId))
    def withPrice(price: BigDecimal): WsState     = copy(price = Some(price))

  def make[F[_]: Async: Logger](
      config: XtbConfig,
      backend: SttpBackend[F, Fs2Streams[F] with WebSockets]
  ): F[XtbClient[F]] =
    Monad[F].pure(LiveXtbClient(config, backend))
