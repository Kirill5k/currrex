package currexx.clients.broker

import cats.Monad
import cats.effect.Async
import currexx.clients.HttpClient
import currexx.clients.broker.vindaloo.VindalooClient
import currexx.domain.market.{CurrencyPair, TradeOrder}

trait BrokerClient[F[_]]:
  def submit(parameters: BrokerParameters, pair: CurrencyPair, order: TradeOrder): F[Unit]

final private class LiveBrokerClient[F[_]](
    private val vindalooClient: VindalooClient[F]
) extends BrokerClient[F]:
  override def submit(parameters: BrokerParameters, pair: CurrencyPair, order: TradeOrder): F[Unit] =
    parameters match
      case BrokerParameters.Vindaloo(externalId) => vindalooClient.submit(externalId, pair, order)

object BrokerClient:
  def make[F[_]: Monad](vindalooClient: VindalooClient[F]): F[BrokerClient[F]] =
    Monad[F].pure(LiveBrokerClient[F](vindalooClient))
