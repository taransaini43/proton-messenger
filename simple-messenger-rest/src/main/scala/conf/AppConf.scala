package conf

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig,HikariDataSource}
import slick.jdbc.SQLiteProfile.api._

import scala.util.Try

trait AppConf {
  val config = ConfigFactory.load("application.conf")

  // db config
  val url = Try(config.getString("sqlite.db.url")).getOrElse("")
  val driver = Try(config.getString("sqlite.db.driver")).getOrElse("")
  val storageDir = Try(config.getString("sqlite.dir")).getOrElse("")


  // db
  val db = {
    val config = new HikariConfig
    config.setDriverClassName(driver)
    config.setJdbcUrl(url)
    config.setConnectionTestQuery("SELECT 1")
    config.setIdleTimeout(10000)
    config.setMaximumPoolSize(20)

    val ds = new HikariDataSource(config)
    Database.forDataSource(ds, Option(20))
  }
}
