import zio._
import zio.test._
import zio.test.Assertion._
import zio.Console._

import java.io.IOException

object DatabaseFoo {
  def countRows(database: MyApp.Database): ZIO[Console with FreshDatabase, Exception, Int] = {
    for {
      _ <- printLine(s"executing SELECT count(*) from ${database.schema}.myTable")
    } yield 0
  }
}

object DatabaseSpec extends DefaultRunnableSpec {
  import DatabaseFoo._

  val cfg: DbConfig = DbConfig("user", "password", "host", 5432, "dbName")

  def spec = suite("DatabaseSpec")(
    test("table should be empty") {
      for {
        db <- FreshDatabase.initializeDatabase(cfg)
        cnt <- countRows(db)
      } yield assert(cnt)(equalTo(0))
    },
    test("table should be empty a 2nd time") {
      for {
        db <- FreshDatabase.initializeDatabase(cfg)
        cnt <- countRows(db)
      } yield assert(cnt)(equalTo(0))
    }
  ).provideLayer(MyApp.testLayer)
}
