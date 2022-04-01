package currexx.core

import cats.effect.{IO, IOApp}
import currexx.core.auth.Auth
import currexx.core.common.config.AppConfig
import currexx.core.common.http.Http
import currexx.core.health.Health
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends IOApp.Simple:
  given log: Logger[IO] = Slf4jLogger.getLogger[IO]
  override val run: IO[Unit] =
    for
      config <- AppConfig.load[IO]
      _ <- Resources.make[IO](config).use { res =>
        for
          health <- Health.make[IO]
          auth   <- Auth.make(config.auth, res)
          http   <- Http.make[IO](health, auth)
          _      <- Server.serve[IO](config.server, http.app, runtime.compute).compile.drain
        yield ()
      }
    yield ()
