package currexx.clients.broker.vindaloo

import cats.Monad
import cats.effect.Temporal
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import currexx.clients.{ClientConfig, HttpClient}
import currexx.clients.broker.{BrokerClient, BrokerParameters}
import currexx.domain.market.Order
import org.typelevel.log4cats.Logger
import sttp.client3.*
import sttp.model.Uri

import scala.concurrent.duration.*

private[clients] trait VindalooClient[F[_]] extends HttpClient[F]:
  def submit(externalId: String, order: Order): F[Unit]

final private class LiveVindalooClient[F[_]](
    private val config: ClientConfig,
    override protected val backend: SttpBackend[F, Any]
)(using
    F: Temporal[F],
    logger: Logger[F]
) extends VindalooClient[F] {

  override protected val name: String                         = "vindaloo"
  override protected val delayBetweenFailures: FiniteDuration = 5.seconds

  override def submit(externalId: String, order: Order): F[Unit] =
    order match
      case enter: Order.EnterMarket => enterMarketOrder(externalId, enter)
      case exit: Order.ExitMarket   => exitMarketOrder(externalId, exit)

  private def enterMarketOrder(externalId: String, order: Order.EnterMarket): F[Unit] = {
    val stopLoss   = order.stopLoss.getOrElse(0)
    val trailingSL = order.trailingStopLoss.getOrElse(0)
    val takeProfit = order.takeProfit.getOrElse(0)
    val pos        = order.position.toString.toLowerCase
    val pair       = s"${order.currencyPair.base.code}${order.currencyPair.quote.code}"
    sendRequest(uri"${config.baseUri}/$externalId/$stopLoss/$trailingSL/$takeProfit/$pos/$pair/${order.volume}")
  }

  private def exitMarketOrder(externalId: String, order: Order.ExitMarket): F[Unit] =
    sendRequest(uri"${config.baseUri}/close/$externalId/${order.currencyPair.base.code}${order.currencyPair.quote.code}")

  private def sendRequest(uri: Uri): F[Unit] =
    dispatch(basicRequest.post(uri))
      .flatMap { r =>
        r.body match {
          case Right(_) => F.unit
          case Left(error) =>
            logger.error(s"$name-client/error-${r.code.code}\n$error") *>
              F.sleep(delayBetweenFailures) *> sendRequest(uri)
        }
      }
}

object VindalooClient:
  def make[F[_]: Temporal: Logger](
      config: ClientConfig,
      backend: SttpBackend[F, Any]
  ): F[VindalooClient[F]] =
    Monad[F].pure(LiveVindalooClient(config, backend))
