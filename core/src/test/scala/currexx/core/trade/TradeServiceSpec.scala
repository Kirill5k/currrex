package currexx.core.trade

import cats.effect.IO
import currexx.clients.broker.{BrokerClient, BrokerParameters}
import currexx.clients.data.MarketDataClient
import currexx.core.CatsSpec
import currexx.core.common.action.{Action, ActionDispatcher}
import currexx.core.fixtures.{Markets, Trades, Users}
import currexx.core.trade.db.{TradeOrderRepository, TradeSettingsRepository}
import currexx.domain.errors.AppError
import currexx.domain.user.UserId
import currexx.domain.market.{CurrencyPair, Indicator, TradeOrder}
import org.mockito.Mockito

import java.time.Instant

class TradeServiceSpec extends CatsSpec {

  "A TradeService" should {
    "getAllOrders" should {
      "return all orders from the repository" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(orderRepo.getAll(any[UserId], anyOpt[Instant], anyOpt[Instant])).thenReturn(IO.pure(List(Trades.order)))

        val result = for
          svc    <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          orders <- svc.getAllOrders(Users.uid, None, Some(Trades.ts))
        yield orders

        result.asserting { res =>
          verify(orderRepo).getAll(Users.uid, None, Some(Trades.ts))
          verifyNoInteractions(settRepo, brokerClient, dataClient, disp)
          res mustBe List(Trades.order)
        }
      }
    }

    "getSettings" should {
      "store trade-settings in the repository" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(settRepo.get(any[UserId])).thenReturn(IO.pure(Trades.settings))

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.getSettings(Users.uid)
        yield ()

        result.asserting { res =>
          verify(settRepo).get(Users.uid)
          verifyNoInteractions(orderRepo, brokerClient, dataClient, disp)
          res mustBe ()
        }
      }

      "return error when settings do not exist" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(settRepo.get(any[UserId])).thenReturn(IO.raiseError(AppError.NotSetup("Trade")))

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.getSettings(Users.uid)
        yield ()

        result.attempt.asserting { res =>
          verify(settRepo).get(Users.uid)
          verifyNoInteractions(orderRepo, brokerClient, dataClient, disp)
          res mustBe Left(AppError.NotSetup("Trade"))
        }
      }
    }

    "updateSettings" should {
      "store trade-settings in the repository" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(settRepo.update(any[TradeSettings])).thenReturn(IO.unit)

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.updateSettings(Trades.settings)
        yield ()

        result.asserting { res =>
          verify(settRepo).update(Trades.settings)
          verifyNoInteractions(orderRepo, brokerClient, dataClient, disp)
          res mustBe ()
        }
      }
    }

    "placeOrder" should {
      "submit order placements" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(dataClient.latestPrice(any[CurrencyPair])).thenReturn(IO.pure(Markets.priceRange))
        when(settRepo.get(any[UserId])).thenReturn(IO.pure(Trades.settings))
        when(brokerClient.submit(any[CurrencyPair], any[BrokerParameters], any[TradeOrder])).thenReturn(IO.unit)
        when(orderRepo.save(any[TradeOrderPlacement])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val order = TradeOrder.Enter(TradeOrder.Position.Buy, BigDecimal(0.1))
        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.placeOrder(Users.uid, Markets.gbpeur, order, false)
        yield ()

        result.asserting { res =>
          verify(dataClient).latestPrice(Markets.gbpeur)
          verify(settRepo).get(Users.uid)
          verify(brokerClient).submit(Markets.gbpeur, Trades.broker, order)
          verify(orderRepo, Mockito.never()).findLatestBy(any[UserId], any[CurrencyPair])
          verify(orderRepo).save(any[TradeOrderPlacement])
          verify(disp).dispatch(any[Action])
          res mustBe ()
        }
      }

      "close existing orders before submitting the actual placement" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(dataClient.latestPrice(any[CurrencyPair])).thenReturn(IO.pure(Markets.priceRange))
        when(settRepo.get(any[UserId])).thenReturn(IO.pure(Trades.settings))
        when(brokerClient.submit(any[CurrencyPair], any[BrokerParameters], any[TradeOrder])).thenReturn(IO.unit)
        when(orderRepo.findLatestBy(any[UserId], any[CurrencyPair])).thenReturn(IO.none)
        when(orderRepo.save(any[TradeOrderPlacement])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val order = TradeOrder.Enter(TradeOrder.Position.Buy, BigDecimal(0.1))
        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.placeOrder(Users.uid, Markets.gbpeur, order, true)
        yield ()

        result.asserting { res =>
          verify(dataClient).latestPrice(Markets.gbpeur)
          verify(settRepo).get(Users.uid)
          verify(brokerClient).submit(Markets.gbpeur, Trades.broker, order)
          verify(orderRepo).findLatestBy(Users.uid, Markets.gbpeur)
          verify(orderRepo).save(any[TradeOrderPlacement])
          verify(disp).dispatch(any[Action])
          res mustBe ()
        }
      }
    }

    "closeOpenOrders" should {
      "not do anything when there are no orders" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(orderRepo.findLatestBy(any[UserId], any[CurrencyPair])).thenReturn(IO.none)

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.closeOpenOrders(Users.uid, Markets.gbpeur)
        yield ()

        result.asserting { res =>
          verify(orderRepo).findLatestBy(Users.uid, Markets.gbpeur)
          verifyNoInteractions(settRepo, brokerClient, dataClient, disp)
          res mustBe ()
        }
      }

      "not do anything when latest order is exit" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(orderRepo.findLatestBy(any[UserId], any[CurrencyPair])).thenReturn(IO.some(Trades.order.copy(order = TradeOrder.Exit)))

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.closeOpenOrders(Users.uid, Markets.gbpeur)
        yield ()

        result.asserting { res =>
          verify(orderRepo).findLatestBy(Users.uid, Markets.gbpeur)
          verifyNoInteractions(settRepo, brokerClient, dataClient, disp)
          res mustBe ()
        }
      }

      "close existing order" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(dataClient.latestPrice(any[CurrencyPair])).thenReturn(IO.pure(Markets.priceRange))
        when(orderRepo.findLatestBy(any[UserId], any[CurrencyPair])).thenReturn(IO.some(Trades.order))
        when(brokerClient.submit(any[CurrencyPair], any[BrokerParameters], any[TradeOrder])).thenReturn(IO.unit)
        when(orderRepo.save(any[TradeOrderPlacement])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.closeOpenOrders(Users.uid, Markets.gbpeur)
        yield ()

        result.asserting { res =>
          verifyNoInteractions(settRepo)
          verify(dataClient).latestPrice(Markets.gbpeur)
          verify(orderRepo).findLatestBy(Users.uid, Markets.gbpeur)
          verify(brokerClient).submit(Markets.gbpeur, Trades.broker, TradeOrder.Exit)
          verify(orderRepo).save(any[TradeOrderPlacement])
          verify(disp).dispatch(any[Action])
          res mustBe ()
        }
      }

      "obtain traded currencies and close all open orders" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(orderRepo.getAllTradedCurrencies(any[UserId])).thenReturn(IO.pure(List(Markets.gbpeur, Markets.gbpusd)))
        when(orderRepo.findLatestBy(any[UserId], any[CurrencyPair])).thenReturn(IO.none)

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.closeOpenOrders(Users.uid)
        yield ()

        result.asserting { res =>
          verify(orderRepo).getAllTradedCurrencies(Users.uid)
          verify(orderRepo).findLatestBy(Users.uid, Markets.gbpusd)
          verify(orderRepo).findLatestBy(Users.uid, Markets.gbpeur)
          verifyNoInteractions(settRepo, brokerClient, dataClient, disp)
          res mustBe ()
        }
      }
    }

    "processMarketStateUpdate" should {
      "not do anything when trading strategy is disabled" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(settRepo.get(any[UserId])).thenReturn(IO.pure(Trades.settings.copy(strategy = TradeStrategy.Disabled)))

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          _   <- svc.processMarketStateUpdate(Markets.state, Markets.trendChangeDetection)
        yield ()

        result.asserting { res =>
          verify(settRepo).get(Users.uid)
          verifyNoInteractions(orderRepo, brokerClient, dataClient, disp)
          res mustBe ()
        }
      }

      "close existing order if new order has reverse position" in {
        val (settRepo, orderRepo, brokerClient, dataClient, disp) = mocks
        when(settRepo.get(any[UserId])).thenReturn(IO.pure(Trades.settings.copy(strategy = TradeStrategy.TrendChange)))
        when(brokerClient.submit(any[CurrencyPair], any[BrokerParameters], any[TradeOrder])).thenReturn(IO.unit)
        when(orderRepo.save(any[TradeOrderPlacement])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val result = for
          svc <- TradeService.make[IO](settRepo, orderRepo, brokerClient, dataClient, disp)
          currentState = Markets.stateWithSignal.copy(currentPosition =
            Some(Markets.positionState.copy(position = TradeOrder.Position.Sell))
          )
          _ <- svc.processMarketStateUpdate(currentState, Markets.trendChangeDetection)
        yield ()

        result.asserting { res =>
          verify(settRepo).get(Users.uid)
          verify(brokerClient).submit(Markets.gbpeur, Trades.broker, TradeOrder.Exit)
          verify(brokerClient).submit(
            Markets.gbpeur,
            Trades.broker,
            Trades.settings.trading.toOrder(Markets.gbpeur, TradeOrder.Position.Buy)
          )
          verify(orderRepo).save(any[TradeOrderPlacement])
          verify(disp).dispatch(any[Action])
          res mustBe ()
        }
      }
    }
  }

  def mocks: (TradeSettingsRepository[IO], TradeOrderRepository[IO], BrokerClient[IO], MarketDataClient[IO], ActionDispatcher[IO]) =
    (
      mock[TradeSettingsRepository[IO]],
      mock[TradeOrderRepository[IO]],
      mock[BrokerClient[IO]],
      mock[MarketDataClient[IO]],
      mock[ActionDispatcher[IO]]
    )
}
