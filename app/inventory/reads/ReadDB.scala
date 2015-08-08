package inventory.reads

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import inventory.events._
import inventory.storage.{EventTx, SqlEventStore}
import inventory.domain.{ItemHelper, Item}

import play.api.{Logger, Play}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.{Failure, Success}

import slick.driver.JdbcProfile


object ReadDB extends HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

  class Items(tag: Tag) extends Table[Item](tag, "items") {
    def id = column[UUID]("id", O.PrimaryKey)

    def name = column[String]("name")

    def quantity = column[Int]("quantity")

    def * = (id, name, quantity) <>(Item.tupled, Item.unapply _)
  }

  val items = TableQuery[Items]

  def handle(eventTx: EventTx) = {
    eventTx.event match {

      case _: ItemCreated | _: ItemSold | _: ItemRestocked | _: ItemArchived =>
        db.run(items.filter(_.id === eventTx.entityId).result).map { dbResult =>
          val upsert =
            if (dbResult.nonEmpty) {
              val update = for {p <- items if p.id === eventTx.entityId} yield (p.name, p.quantity)
              val item = dbResult.head
              Success(update +=(item.name, item.quantity))
            } else {
              ItemHelper.itemEventHandler(eventTx.event)(None).map { item =>
                items += item.copy(id = eventTx.entityId)
              }

            }

          upsert match {
            case Success(dbOperation) => db.run(dbOperation)
            case Failure(error) => Logger.error("unable to apply eventTx: " + eventTx, error)
          }
        }

      case _ => Logger.debug("discarding: " + eventTx)
    }
  }
}

object EventStoreSubscriber {

  implicit val system = ActorSystem("event-store")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()
  def init {
    Logger.debug("init") // TODO: need to figure out why sink does not run w/o object being directly referenced
  }
  SqlEventStore.source.runWith(Sink.foreach{ eventTx =>
    Logger.debug("got event from eventstore " + eventTx)

    ReadDB.handle(eventTx)
  })

}