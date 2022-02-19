import zio._
import zio.Console._
import zio.Clock._
import java.util.concurrent.TimeUnit
import java.util.UUID

object MyApp extends zio.ZIOAppDefault {

  case class Testname(uuid: UUID)
  case class Databasename(name: String, connectionstring: String)

  val myApp: ZIO[Console with RandomTestName, Nothing, Unit] = (
    for {
      testname <- RandomTestName.generateTestname
      _ <- printLine(
        s"Performing Test in fresh environment with name '$testname'"
      )
    } yield ()
  ).orDie

  def run = mainEffect

  val mainEffect: ZIO[Any, Nothing, Unit] =
    myApp.provide(Random.live, Console.live, RandomTestNameLive.layer)

}

trait RandomTestName {
  def generateTestname: UIO[MyApp.Testname]
}

object RandomTestName {
  def generateTestname: URIO[RandomTestName, MyApp.Testname] =
    ZIO.serviceWithZIO[RandomTestName](_.generateTestname)

}

case class RandomTestNameLive(random: Random) extends RandomTestName {
  override def generateTestname: UIO[MyApp.Testname] =
    random.nextUUID.map(MyApp.Testname)
}

object RandomTestNameLive {
  val layer: URLayer[Random, RandomTestName] =
    (RandomTestNameLive(_)).toLayer[RandomTestName]

}
