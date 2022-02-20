package de.flwi.envtest.catseffectio

import cats.effect._
import weaver._

import java.util.UUID

object Helper {}

object Test extends SimpleIOSuite {

  override def sharedResource: Resource[IO, Unit] = super.sharedResource

  test("hello side effects") {
    TestEnvironment.testResource.use(BusinessLogic.getResult).map(res => expect(res == 42))
  }

  test("hello side effects 2") {
    TestEnvironment.testResource.use(BusinessLogic.getResult).map(res => expect(res == 42))
  }
}

case class TestEnvironment(uuid: UUID)

object TestEnvironment {

  val testEnvResource: Resource[IO, TestEnvironment] = Resource.eval(IO(TestEnvironment(UUID.randomUUID())))

  val dockerResource: TestEnvironment => Resource[IO, DockerConfig] = env =>
    Resource
      .pure(
        DockerConfig(
          host = "host",
          port = 9092,
          topicName = env.uuid.toString.replace("-", "_")
        )
      )
      .evalTap(createTopic)

  def createTestSchema(database: DatabaseConfig): IO[Unit] = IO {
    println(s"creating schema ${database.schema}")
  }

  def createTopic(dockerConfig: DockerConfig): IO[Unit] =
    IO(println(s"creating topic ${dockerConfig.topicName}"))

  val dbResource: TestEnvironment => Resource[IO, DatabaseConfig] = env =>
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

  val testResource: Resource[IO, BusinessLogicConfig] = for {
    env <- testEnvResource
    db <- dbResource(env)
    docker <- dockerResource(env)
  } yield BusinessLogicConfig(db, docker)

}
