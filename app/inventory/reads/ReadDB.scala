package inventory.reads

import java.util.UUID
import javax.inject.Inject

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import inventory.events._
import inventory.storage.EventTx
import inventory.domain.{Item, ItemHelper}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcProfile


class ReadDB @Inject() (dbConfigProvider: DatabaseConfigProvider,
                        implicit val ec: ExecutionContext) extends HasDatabaseConfig[JdbcProfile] {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.profile.api._

  class Items(tag: Tag) extends Table[Item](tag, "items") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def quantity = column[Int]("quantity")

    def * = (id, name, quantity) <>(Item.tupled, Item.unapply _)
  }

  val items = TableQuery[Items]

  def insert(item: Item) = db.run(
    items += item
  )

  def update(item: Item) = {
    val existing = for {i <- items if i.id === item.id} yield (i.name, i.quantity)
    db.run(existing.update(item.name, item.quantity))
  }

  def delete(item: Item) = {
    db.run(items.filter(_.id === item.id).delete)
  }

  def get(id: UUID) = {
    db.run(items.filter(_.id === id).result).flatMap { dbResult =>
      if (dbResult.nonEmpty && dbResult.size == 1) {
        Future.successful(dbResult.headOption)
      } else {
        Future.failed(new RuntimeException("expected 1 result from db, " + dbResult))
      }
    }
  }

  def getAll = db.run(
    items.result
  )

  def handle(eventTx: EventTx) = {
    import Future._
    import ItemHelper._

    eventTx.event match {
      case _: ItemCreated =>
        for {
          newItem <- fromTry(itemEventHandler(eventTx.event)(None))
          result <- insert(newItem.copy(id = eventTx.entityId))
        } yield result

      case _: ItemReduced | _: ItemIncreased =>
        for {
          existingItem <- get(eventTx.entityId)
          updatedItem <- fromTry(itemEventHandler(eventTx.event)(existingItem))
          result <- update(updatedItem)
        } yield result

      case archived: ItemArchived =>
        for {
          existingItem <- get(eventTx.entityId)
          archivedItem <- fromTry(itemEventHandler(eventTx.event)(existingItem))
          result <- delete(archivedItem)
        } yield result

      case _ =>
        Future.failed(new RuntimeException("not handling " + eventTx))
    }

  }

}

class EventStoreSubscriber @Inject() (readDB: ReadDB) {

  implicit val system = ActorSystem("event-store")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  def subscribe(source: Source[EventTx, NotUsed]) {
    source.runWith(Sink.foreach{ eventTx =>
      Logger.debug("got event from eventstore " + eventTx)

      readDB.handle(eventTx)
    })
  }

}