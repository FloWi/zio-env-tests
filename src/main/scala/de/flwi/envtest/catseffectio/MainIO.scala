package de.flwi.envtest.catseffectio

import cats.effect._

case class DatabaseConfig(
    user: String,
    password: String,
    host: String,
    port: Int,
    dbName: String,
    schema: String
) {
  val connectionString =
    s"jdbc:postgresql://$user:$password@$host:$port/$dbName"
  val connectionStringWithSchema =
    s"$connectionString?currentSchema=$schema"
}

case class KafkaConfig(host: String, port: Int, topicName: String)

object BusinessLogic {

  def getResult(cfg: BusinessLogicConfig): IO[Int] = {
    IO {
      println(s"consuming from topic ${cfg.docker.topicName}")
      println(s"executing INSERT into ${cfg.database.schema}.myTable")
      42
    }
  }

  // i need BL, but s.o. has to provide it - I don't care, when that happens

}
case class BusinessLogicConfig(database: DatabaseConfig, docker: KafkaConfig)

object MainIO extends IOApp {

  val dbResource: Resource[IO, DatabaseConfig] = Resource.pure(
    DatabaseConfig(
      user = "user-from-cfg-file",
      password = "password-from-cfg-file",
      host = "host-from-cfg-file",
      port = 5432,
      dbName = "dbName-from-cfg-file",
      schema = "schema-from-cfg-file"
    )
  )

  val dockerResource: Resource[IO, KafkaConfig] = Resource.pure(
    KafkaConfig(
      host = "host-from-cfg-file",
      port = 9092,
      topicName = "kafka-topic-from-cfg-file"
    )
  )

  val mainResource: Resource[IO, BusinessLogicConfig] = for {
    db <- dbResource
    docker <- dockerResource
  } yield BusinessLogicConfig(db, docker)

  override def run(args: List[String]): IO[ExitCode] =
    mainResource.use(BusinessLogic.getResult).as(ExitCode.Success)
}
