import zio._
import zio.Console._
import zio.Clock._
import java.util.concurrent.TimeUnit
import java.util.UUID

object MyApp extends zio.ZIOAppDefault {

  case class TestEnvironment(uuid: UUID)
  case class Database(dbConfig: DbConfig, schema: String) {
    val connectionString =
      s"jdbc:postgresql://${dbConfig.user}:${dbConfig.password}@${dbConfig.host}:${dbConfig.port}/${dbConfig.dbName}"
    val connectionStringWithSchema =
      s"$connectionString?currentSchema=${schema}"
  }

  def myTest(
      testname: String,
      cfg: DbConfig
  ): ZIO[Console with RandomTestEnvironment with FreshDatabase, Nothing, Unit] =
    (
      for {
        testEnvironmentName <- RandomTestEnvironment.generateTestEnvironmentName
        db <- FreshDatabase.initializeDatabase(cfg)
        _ <- printLine(s"Performing Test '$testname' in environment with name '$testEnvironmentName'")
        _ <- printLine(s"  connectionString: ${db.connectionStringWithSchema}")

      } yield ()
    ).orDie

  def run = {
    val cfg = DbConfig("user", "password", "host", 5432, "dbName")
    (myTest("Test1", cfg) *>
      myTest("Test2", cfg)).provide(testLayer)
  }

  val testLayer: ZLayer[
    Any,
    Nothing,
    Console with RandomTestEnvironment with FreshDatabase
  ] =
    Random.live >>> (Console.live >+> RandomTestEnvironmentLive.layer >+> FreshDatabaseLive.layer)
}

trait RandomTestEnvironment {
  def generateTestEnvironmentName: UIO[MyApp.TestEnvironment]
}

object RandomTestEnvironment {
  def generateTestEnvironmentName: URIO[RandomTestEnvironment, MyApp.TestEnvironment] =
    ZIO.serviceWithZIO[RandomTestEnvironment](_.generateTestEnvironmentName)

}

case class RandomTestEnvironmentLive(random: Random) extends RandomTestEnvironment {
  override def generateTestEnvironmentName: UIO[MyApp.TestEnvironment] =
    random.nextUUID.map(MyApp.TestEnvironment)
}

object RandomTestEnvironmentLive {
  val layer: URLayer[Random, RandomTestEnvironment] =
    (RandomTestEnvironmentLive(_)).toLayer[RandomTestEnvironment]
}

case class DbConfig(
    user: String,
    password: String,
    host: String,
    port: Int,
    dbName: String
)

trait FreshDatabase {
  def initializeDatabase(
      dbConfig: DbConfig
  ): RIO[Console with RandomTestEnvironment, MyApp.Database]
}

object FreshDatabase {
  def initializeDatabase(cfg: DbConfig) =
    ZIO.serviceWithZIO[FreshDatabase](_.initializeDatabase(cfg))
}

case class FreshDatabaseLive(testEnvironment: RandomTestEnvironment) extends FreshDatabase {
  override def initializeDatabase(
      dbConfig: DbConfig
  ): RIO[Console with RandomTestEnvironment, MyApp.Database] = {

    RandomTestEnvironment.generateTestEnvironmentName.flatMap { testenv =>
      val schemaname = testenv.uuid.toString().replace("-", "_")
      val database = MyApp.Database(dbConfig, schemaname)

      for {
        _ <- printLine(s"   Creating schema for testenvironment ${testenv.uuid}")
        _ <- printLine(s"   created schema '$schemaname'")
        _ <- printLine(s"   connectionstring: '${database.connectionStringWithSchema}'")
        _ <- printLine(s"   performing flyway migrations...")
      } yield database
    }
  }
}

object FreshDatabaseLive {
  val layer: URLayer[RandomTestEnvironment, FreshDatabase] =
    (FreshDatabaseLive(_)).toLayer[FreshDatabase]
}
