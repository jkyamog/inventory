package inventory.reads

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import inventory.events._
import inventory.storage.SqlEventStore

import play.api.{Logger, Play}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import inventory.domain.{ProductHelper, Product}

import scala.util.{Failure, Success}

object Products extends HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

  class Products(tag: Tag) extends Table[Product](tag, "products") {
    def id = column[UUID]("id", O.PrimaryKey)
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
    Logger.debug("got event from eventstore " + eventTx)

    eventTx.event match {

      case _: ProductCreated | _: ProductSold | _: ProductRestocked | _: ProductArchived =>
        db.run(products.filter(_.id === eventTx.entityId).result).map { productsResult =>
          val upsert =
            if (productsResult.nonEmpty) {
              val update = for {p <- products if p.id === eventTx.entityId} yield (p.name, p.quantity)
              val product = productsResult.head
              Success(update +=(product.name, product.quantity))
            } else {
              ProductHelper.productEvents(eventTx.event)(None).map { product =>
                products += product.copy(id = eventTx.entityId)
              }

            }

          upsert match {
            case Success(dbOperation) => db.run(dbOperation)
            case Failure(error) => Logger.error("unable to apply eventTx: " + eventTx, error)
          }
        }

      case _ => Logger.debug("discarding: " + eventTx)
    }
  })

}