package conf

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.jdbc.SQLiteProfile.api._

import java.io.File
import scala.util.Try

trait AppConf {
  val config = ConfigFactory.load("application.conf")

  // db config
  val url = Try(config.getString("sqlite.db.url")).getOrElse("")
  val driver = Try(config.getString("sqlite.db.driver")).getOrElse("")

  val dbPath: String = sys.env.getOrElse("sqlite.db.path","/home/app/sqlite-dir/localdatabase.db")
  val storageDir: String = dbPath.substring(0, dbPath.lastIndexOf("/") + 1)
  println(storageDir)
  println(dbPath)

  // Ensure the directory exists
  def ensureDirectoryExists(): Unit = {
    val dir = new File(storageDir)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

  // db
  val db = {
    ensureDirectoryExists()
    val config = new HikariConfig
    config.setDriverClassName(driver)
    config.setJdbcUrl(url+dbPath)
    config.setConnectionTestQuery("SELECT 1")
    config.setIdleTimeout(10000)
    config.setMaximumPoolSize(20)

    val ds = new HikariDataSource(config)
    Database.forDataSource(ds, Option(20))
  }
}
