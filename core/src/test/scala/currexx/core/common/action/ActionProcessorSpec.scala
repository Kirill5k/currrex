package currexx.core.common.action

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import currexx.core.CatsSpec
import currexx.core.fixtures.Signals
import currexx.core.monitor.MonitorService
import currexx.core.signal.SignalService

import scala.concurrent.duration.*

class ActionProcessorSpec extends CatsSpec {

  "An ActionProcessor" should {
    "process submitted signals" in {
      val (monsvc, sigsvc) = mocks
      val result = for {
        dispatcher <- ActionDispatcher.make[IO]
        processor  <- ActionProcessor.make[IO](dispatcher, monsvc, sigsvc)
        _          <- dispatcher.dispatch(Action.SignalSubmitted(Signals.macd))
        res        <- processor.run.interruptAfter(2.second).compile.drain
      } yield res

      result.unsafeToFuture().map { r =>
        r mustBe ()
      }
    }
  }

  def mocks: (MonitorService[IO], SignalService[IO]) =
    (mock[MonitorService[IO]], mock[SignalService[IO]])
}
