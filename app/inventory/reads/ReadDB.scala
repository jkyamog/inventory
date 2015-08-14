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
import slick.profile.FixedSqlAction

import scala.concurrent.Future
import scala.util.{Failure, Success}

import slick.driver.JdbcProfile


object ReadDB extends HasDatabaseConfig[JdbcProfile] {
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

  def handle(eventTx: EventTx): Future[FixedSqlAction[Int, NoStream, Effect.Write]] = {
    eventTx.event match {
      case _: ItemCreated | _: ItemSold | _: ItemRestocked | _: ItemArchived =>
        db.run(items.filter(_.id === eventTx.entityId).result).flatMap { dbResult =>
          val upsert =
            if (dbResult.nonEmpty) {
              val existing = for {p <- items if p.id === eventTx.entityId} yield (p.name, p.quantity)
              ItemHelper.itemEventHandler(eventTx.event)(dbResult.headOption).map { item =>
                existing.update(item.name, item.quantity)
              }
            } else {
              ItemHelper.itemEventHandler(eventTx.event)(None).map { item =>
                items += item.copy(id = eventTx.entityId)
              }
            }
          Future.fromTry(upsert)
        }

      case _ =>
        Future.failed(new RuntimeException("not handling " + eventTx))
    }
  }

  def doDbAction(dbAction: Future[DBIOAction[_, NoStream, Nothing]]) = {
    dbAction.onComplete {
      case Success(dbOperation) =>
        Logger.debug(s"dbOperation " + dbOperation)
        db.run(dbOperation)

      case Failure(error) =>
        Logger.error("dboperation failed", error)
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

    val dbAction = ReadDB.handle(eventTx)
    ReadDB.doDbAction(dbAction)
  })

}