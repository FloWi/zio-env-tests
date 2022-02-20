import BusinessLogic.getResult
import zio._
import zio.Console._
import zio.internal.stacktracer.Tracer

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

case class DockerConfig(host: String, port: Int, topicName: String)

trait BusinessLogic {
  def getResult: UIO[Int]
}

object BusinessLogic {

  // i need BL, but s.o. has to provide it - I don't care, when that happens
  val any: ZLayer[BusinessLogic, Nothing, BusinessLogic] =
    ZLayer.service[BusinessLogic](Tag[BusinessLogic], Tracer.newTrace)

  val live: URLayer[Console with DatabaseConfig with DockerConfig, BusinessLogic] = {
    (BusinessLogicLive(_, _, _)).toLayer[BusinessLogic]
  }

  case class BusinessLogicLive(console: Console, database: DatabaseConfig, docker: DockerConfig) extends BusinessLogic {
    override def getResult: UIO[Int] = {

      console.printLine(s"consuming from topic ${docker.topicName}").orDie *>
        console.printLine(s"executing INSERT into ${database.schema}.myTable").orDie.as(42)
    }
  }

  def getResult: RIO[BusinessLogic, Int] = ZIO.serviceWithZIO(_.getResult)
}

object MyApp extends zio.ZIOAppDefault {

  def myApp: ZIO[BusinessLogic, Throwable, Int] = for {
    res <- getResult
  } yield res

  val dbLayer: ULayer[DatabaseConfig] = ZLayer.succeed(
    DatabaseConfig(
      user = "user-from-cfg-file",
      password = "password-from-cfg-file",
      host = "host-from-cfg-file",
      port = 5432,
      dbName = "dbName-from-cfg-file",
      schema = "schema-from-cfg-file"
    )
  )

  val dockerLayer: ULayer[DockerConfig] = ZLayer.succeed(
    DockerConfig(
      host = "host-from-cfg-file",
      port = 9092,
      topicName = "kafka-topic-from-cfg-file"
    )
  )

  val mainLayer: ZLayer[Any, Nothing, BusinessLogic] =
    Console.live ++ dbLayer ++ dockerLayer >>> BusinessLogic.live

  override def run: ZIO[ZEnv with ZIOAppArgs, Any, Any] = for {
    res <- myApp.provide(mainLayer)
    _ <- printLine(res)
  } yield ()
}
