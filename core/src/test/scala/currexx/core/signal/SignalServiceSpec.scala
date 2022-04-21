package currexx.core.signal

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import currexx.core.CatsSpec
import currexx.domain.user.UserId
import currexx.domain.market.{Condition, CurrencyPair, Indicator}
import currexx.core.common.action.{Action, ActionDispatcher}
import currexx.core.fixtures.{Markets, Signals, Users}
import currexx.core.signal.db.{SignalRepository, SignalSettingsRepository}

class SignalServiceSpec extends CatsSpec {

  "A SignalService" when {
    "getSettings" should {
      "store signal-settings in the repository" in {
        val (signRepo, settRepo, disp) = mocks
        when(settRepo.get(any[UserId], any[CurrencyPair])).thenReturn(IO.pure(Some(Signals.settings)))

        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          _   <- svc.getSettings(Users.uid, Markets.gbpeur)
        yield ()

        result.unsafeToFuture().map { res =>
          verify(settRepo).get(Users.uid, Markets.gbpeur)
          verifyNoInteractions(signRepo, disp)
          res mustBe ()
        }
      }
    }

    "updateSettings" should {
      "store signal-settings in the repository" in {
        val (signRepo, settRepo, disp) = mocks
        when(settRepo.update(any[SignalSettings])).thenReturn(IO.unit)

        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          _   <- svc.updateSettings(Signals.settings)
        yield ()

        result.unsafeToFuture().map { res =>
          verify(settRepo).update(Signals.settings)
          verifyNoInteractions(signRepo, disp)
          res mustBe ()
        }
      }
    }

    "submit" should {
      "store new signal in the repository and dispatch an action" in {
        val (signRepo, settRepo, disp) = mocks
        when(signRepo.save(any[Signal])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          _   <- svc.submit(Signals.macd)
        yield ()

        result.unsafeToFuture().map { res =>
          verify(signRepo).save(Signals.macd)
          verify(disp).dispatch(Action.SignalSubmitted(Signals.macd))
          res mustBe ()
        }
      }
    }

    "getAll" should {
      "return all signals from the signRepository" in {
        val (signRepo, settRepo, disp) = mocks
        when(signRepo.getAll(any[UserId])).thenReturn(IO.pure(List(Signals.macd)))

        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          res <- svc.getAll(Users.uid)
        yield res

        result.unsafeToFuture().map { res =>
          verifyNoInteractions(settRepo, disp)
          verify(signRepo).getAll(Users.uid)
          res mustBe List(Signals.macd)
        }
      }
    }

    "processMarketData" should {
      "not do anything when there are no changes in market data since last point" in {
        val (signRepo, settRepo, disp) = mocks
        when(settRepo.get(any[UserId], any[CurrencyPair])).thenReturn(IO.pure(Some(Signals.settings)))

        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          res <- svc.processMarketData(Users.uid, Markets.timeSeriesData.copy(prices = Markets.priceRanges))
        yield res

        result.unsafeToFuture().map { res =>
          verify(settRepo).get(Users.uid, Markets.gbpeur)
          verifyNoInteractions(disp, signRepo)
          res mustBe ()
        }
      }

      "insert default signal settings when these are missing" in {
        val (signRepo, settRepo, disp) = mocks
        when(settRepo.get(any[UserId], any[CurrencyPair])).thenReturn(IO.pure(None))
        when(settRepo.update(any[SignalSettings])).thenReturn(IO.unit)

        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          res <- svc.processMarketData(Users.uid, Markets.timeSeriesData.copy(prices = Markets.priceRanges))
        yield res

        result.unsafeToFuture().map { res =>
          verify(settRepo).get(Users.uid, Markets.gbpeur)
          verify(settRepo).update(Signals.settings)
          verifyNoInteractions(disp, signRepo)
          res mustBe ()
        }
      }

      "detect MACD line crossing up" in {
        val (signRepo, settRepo, disp) = mocks
        when(settRepo.get(any[UserId], any[CurrencyPair])).thenReturn(IO.pure(Some(Signals.settings)))
        when(signRepo.save(any[Signal])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val timeSeriesData = Markets.timeSeriesData.copy(prices = Markets.priceRanges.drop(2))
        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          res <- svc.processMarketData(Users.uid, timeSeriesData)
        yield res

        result.unsafeToFuture().map { res =>
          val expectedSignal = Signal(Users.uid, Markets.gbpeur, Indicator.MACD, Condition.CrossingUp, timeSeriesData.prices.head.time)
          verify(settRepo).get(Users.uid, Markets.gbpeur)
          verify(signRepo).save(expectedSignal)
          verify(disp).dispatch(Action.SignalSubmitted(expectedSignal))
          res mustBe ()
        }
      }

      "detect MACD line crossing down" in {
        val (signRepo, settRepo, disp) = mocks
        when(settRepo.get(any[UserId], any[CurrencyPair])).thenReturn(IO.pure(Some(Signals.settings)))
        when(signRepo.save(any[Signal])).thenReturn(IO.unit)
        when(disp.dispatch(any[Action])).thenReturn(IO.unit)

        val timeSeriesData = Markets.timeSeriesData.copy(prices = Markets.priceRanges.drop(5))
        val result = for
          svc <- SignalService.make[IO](signRepo, settRepo, disp)
          res <- svc.processMarketData(Users.uid, timeSeriesData)
        yield res

        result.unsafeToFuture().map { res =>
          val expectedSignal = Signal(Users.uid, Markets.gbpeur, Indicator.MACD, Condition.CrossingDown, timeSeriesData.prices.head.time)
          verify(settRepo).get(Users.uid, Markets.gbpeur)
          verify(signRepo).save(expectedSignal)
          verify(disp).dispatch(Action.SignalSubmitted(expectedSignal))
          res mustBe ()
        }
      }
    }
  }

  def mocks: (SignalRepository[IO], SignalSettingsRepository[IO], ActionDispatcher[IO]) =
    (mock[SignalRepository[IO]], mock[SignalSettingsRepository[IO]], mock[ActionDispatcher[IO]])

  extension [A](nel: NonEmptyList[A])
    def drop(n: Int): NonEmptyList[A] =
      NonEmptyList.fromListUnsafe(nel.toList.drop(n))
}
