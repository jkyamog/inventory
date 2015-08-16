package inventory.reads

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Sink}

import inventory.events._
import inventory.storage.{EventTx, SqlEventStore}
import inventory.domain.{ItemHelper, Item}

import play.api.{Logger, Play}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.profile.FixedSqlAction

import scala.concurrent.Future
import scala.util.{Failure, Success}

import slick.driver.JdbcProfile


class ReadDB extends HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

  class Items(tag: Tag) extends Table[Item](tag, "items") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def quantity = column[Int]("quantity")
    def archived = column[Option[Boolean]]("archived")

    def * = (id, name, quantity, archived) <>(Item.tupled, Item.unapply _)
  }

  val items = TableQuery[Items]

  def insert(item: Item) = db.run(
    items += item
  )

  def update(item: Item) = {
    val existing = for {i <- items if i.id === item.id} yield (i.name, i.quantity)
    db.run(existing.update(item.name, item.quantity))
  }

  def get(id: UUID) = db.run(
    items.filter(_.id === id).result.headOption
  )

  def handle(eventTx: EventTx) = {
    val f = eventTx.event match {
      case _: ItemCreated =>
        Future.fromTry(
          ItemHelper.itemEventHandler(eventTx.event)(None).map { item =>
            insert(item.copy(id = eventTx.entityId))
          })

      case _: ItemSold | _: ItemRestocked | _: ItemArchived =>
        db.run(items.filter(_.id === eventTx.entityId).result).flatMap { dbResult =>
          Future.fromTry(
            if (dbResult.nonEmpty && dbResult.size == 1) {
              ItemHelper.itemEventHandler(eventTx.event)(dbResult.headOption).map { item =>
                update(item)
              }
            } else {
              Failure(new RuntimeException("expected 1 result from db, " + dbResult))
            })
        }

      case _ =>
        Future.failed(new RuntimeException("not handling " + eventTx))
    }

    f.flatMap(f => f)
  }

}

class EventStoreSubscriber {

  implicit val system = ActorSystem("event-store")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val readDB = new ReadDB

  def subscribe(source: Source[EventTx, Unit]) {
    source.runWith(Sink.foreach{ eventTx =>
      Logger.debug("got event from eventstore " + eventTx)

      readDB.handle(eventTx)
    })
  }

}