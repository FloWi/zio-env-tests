import zio._
import zio.test._

import java.util.UUID

object DatabaseSpec extends DefaultRunnableSpec {
  val testLayer: ZLayer[Any, Nothing, BusinessLogic] =
    Random.live >>> TestEnvironment.random >>> (Console.live >+> TestDatabaseEnvironment.testDbLayer >>> BusinessLogic.live)

  val cfg: DbConfig = DbConfig("user", "password", "host", 5432, "dbName")

  def spec = suite("DatabaseSpec")(
    test("table should be empty") {
      for {
        res <- BusinessLogic.getResult
      } yield assertTrue(res == 42)
    },
    test("table should be empty for a 2nd time") {
      for {
        res <- BusinessLogic.getResult
      } yield assertTrue(res == 42)
    }
  ).provideLayer(testLayer)
}

case class TestEnvironment(uuid: UUID)

object TestEnvironment {
  val random: ZLayer[Random, Nothing, TestEnvironment] = ZLayer.fromZIO(zio.Random.nextUUID.map(TestEnvironment(_)))

  val uuid: URIO[TestEnvironment, UUID] = ZIO.serviceWith[TestEnvironment](_.uuid)
}

object TestDatabaseEnvironment {

  def calcDbConfig(testEnvironment: TestEnvironment): Database =
    Database(user = "user", password = "password", host = "host", port = 5432, dbName = "dbName", schema = testEnvironment.uuid.toString.replace("-", "_"))
  def createTestSchema(database: Database): URIO[Console, Unit] =
    zio.Console.printLine(s"creating schema ${database.schema}").orDie

  def performFlywayMigration(database: Database): URIO[Console, Unit] =
    zio.Console.printLine((s"performing flyway migration for schema ${database.schema}")).orDie

  val setupDb: URIO[Console with TestEnvironment, Database] = {
    for {
      env <- ZIO.service[TestEnvironment]
      db <- UIO.succeed(calcDbConfig(env))
      _ <- createTestSchema(db)
      _ <- performFlywayMigration(db)
    } yield db
  }

  val testDbLayer: ZLayer[Console with TestEnvironment, Nothing, Database] = ZLayer.fromZIO(setupDb)
}
