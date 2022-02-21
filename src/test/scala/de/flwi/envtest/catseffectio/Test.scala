package de.flwi.envtest.catseffectio

import cats.effect._
import weaver._

import java.util.UUID

trait MySuite extends SimpleIOSuite {

  type PerTestRes
  def perTestResource: Resource[IO, PerTestRes]

  def myTest(testName: TestName)(run: (PerTestRes, Log[IO]) => IO[Expectations]): Unit =
    test(testName)((_, log) => perTestResource.use(run(_, log)))
}

object Test extends MySuite {

  myTest("hello side effects") { (env, _) =>
    BusinessLogic.getResult(env).map(res => expect(res == 42))
  }

  myTest("hello side effects 2") { (env, _) =>
    BusinessLogic.getResult(env).map(res => expect(res == 42))
  }

  override type PerTestRes = BusinessLogicConfig

  override def perTestResource: Resource[IO, Test.PerTestRes] = TestId.testEnvironmentResource

}

case class TestId(uuid: UUID)

object TestId {

  val testIdResource: Resource[IO, TestId] = Resource.eval(IO(println("generating random test id")) *> IO(TestId(UUID.randomUUID())))

  val kafkaResource: TestId => Resource[IO, KafkaConfig] = env =>
    Resource
      .pure(
        KafkaConfig(
          host = "host",
          port = 9092,
          topicName = env.uuid.toString.replace("-", "_")
        )
      )
      .evalTap(createTopic)

  def createTopic(kafkaConfig: KafkaConfig): IO[Unit] =
    IO(println(s"creating topic ${kafkaConfig.topicName}"))

  val dbResource: TestId => Resource[IO, DatabaseConfig] = env =>
    Resource
      .pure(
        DatabaseConfig(
          user = "user",
          password = "password",
          host = "host",
          port = 5432,
          dbName = "dbName",
          schema = env.uuid.toString.replace("-", "_")
        )
      )
      .evalTap(createTestSchema)

  def createTestSchema(database: DatabaseConfig): IO[Unit] = IO {
    println(s"creating schema ${database.schema}")
  }

  val testEnvironmentResource: Resource[IO, BusinessLogicConfig] = for {
    env <- testIdResource
    db <- dbResource(env)
    kafka <- kafkaResource(env)
  } yield BusinessLogicConfig(db, kafka)
}
