package inventory.reads

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import inventory.storage.{EventTx, SqlEventStore}

import play.api.{Logger, Play}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import inventory.domain.{ProductHelper, Product}

object Products extends HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

  class Products(tag: Tag) extends Table[Product](tag, "products") {
    def id = column[Long]("id", O.PrimaryKey)
    def name = column[String]("name")
    def quantity = column[Int]("quantity")

    def * = (id, name, quantity) <> (Product.tupled, Product.unapply _)
  }

  val products = TableQuery[Products]

  implicit val system = ActorSystem("event-store")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()
  def init {
    Logger.debug("init products") // TODO: need to figure out why sink does not run w/o object being directly referenced
  }
  SqlEventStore.source.runWith(Sink.foreach{ eventTx => //.runForeach{ eventTx =>
    Logger.debug("got event from eventstore")

    db.run(products.filter(_.id === eventTx.entityId).result).map { productsResult =>
      val upsert =
        if (productsResult.nonEmpty) {
          val update = for {p <- products if p.id === eventTx.entityId} yield (p.name, p.quantity)
          val product = productsResult.head
          update += (product.name, product.quantity)
        } else {
          val product = ProductHelper.productEvents(eventTx.event, None)
          products += product.copy(id = eventTx.entityId)
        }

      db.run(upsert)
    }
  })

}