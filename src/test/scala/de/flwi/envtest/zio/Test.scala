package de.flwi.envtest.zio

import zio._
import zio.test._

import java.util.UUID

object Test extends DefaultRunnableSpec {
  val testLayer: ZLayer[Any, Nothing, BusinessLogic] =
    Random.live >>> TestEnvironment.random >>> (Console.live >+> TestDatabaseEnvironment.testDbLayer >+> TestDockerEnvironment.testDockerLayer >>> BusinessLogic.live)

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

  def calcDbConfig(testEnvironment: TestEnvironment): DatabaseConfig =
    DatabaseConfig(
      user = "user",
      password = "password",
      host = "host",
      port = 5432,
      dbName = "dbName",
      schema = testEnvironment.uuid.toString.replace("-", "_")
    )
  def createTestSchema(database: DatabaseConfig): URIO[Console, Unit] =
    zio.Console.printLine(s"creating schema ${database.schema}").orDie

  def performFlywayMigration(database: DatabaseConfig): URIO[Console, Unit] =
    zio.Console.printLine((s"performing flyway migration for schema ${database.schema}")).orDie

  val setupDb: URIO[Console with TestEnvironment, DatabaseConfig] = {
    for {
      env <- ZIO.service[TestEnvironment]
      db <- UIO.succeed(calcDbConfig(env))
      _ <- createTestSchema(db)
      _ <- performFlywayMigration(db)
    } yield db
  }

  val testDbLayer: ZLayer[Console with TestEnvironment, Nothing, DatabaseConfig] = ZLayer.fromZIO(setupDb)
}

object TestDockerEnvironment {

  def calcDockerConfig(testEnvironment: TestEnvironment): DockerConfig =
    DockerConfig(
      host = "host",
      port = 9092,
      topicName = testEnvironment.uuid.toString.replace("-", "_")
    )

  def createTopic(dockerConfig: DockerConfig): URIO[Console, Unit] =
    zio.Console.printLine(s"creating topic ${dockerConfig.topicName}").orDie

  val setupTopic: URIO[Console with TestEnvironment, DockerConfig] = {
    for {
      env <- ZIO.service[TestEnvironment]
      dockerCfg <- UIO.succeed(calcDockerConfig(env))
      _ <- createTopic(dockerCfg)
    } yield dockerCfg
  }

  val testDockerLayer: ZLayer[Console with TestEnvironment, Nothing, DockerConfig] = ZLayer.fromZIO(setupTopic)
}
