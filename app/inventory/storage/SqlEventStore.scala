package inventory.storage

import java.util.UUID

import inventory.events.Event

import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile


class SqlEventStore extends EventStore with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._
  import JsonFormatter.eventFormatter

  case class EventData(txId: Option[Long], entityId: UUID, event: String)

  class Events(tag: Tag) extends Table[EventData](tag, "events") {

    def txId = column[Long]("tx_id", O.PrimaryKey, O.AutoInc)
    def entityId = column[UUID]("entity_id")
    def event = column[String]("event")

    def * = (txId.?, entityId, event) <> (EventData.tupled, EventData.unapply _)
  }

  val events = TableQuery[Events]

  def storeEvent(entityId: UUID, event: Event) = {
    val eventJson = Json.toJson(event)
    val eventData = EventData(None, entityId, eventJson.toString)

    db.run(
      events.returning(events.map(_.txId)) += eventData
    )
  }

  def getEvents(entityId: UUID) = {
    val eventsFromDb = db.run(
      events.sortBy(_.txId).filter(_.entityId === entityId).result
    )

    eventsFromDb.map { events =>
      val jsons = events.map(e => Json.parse(e.event))
      jsons.map{ json =>
        Json.fromJson[Event](json).get
      }
    }

  }

  def allEvents = {
    db.stream(
      events.sortBy(_.txId).result
    ).mapResult { eventData =>
      Json.parse(eventData.event)
    }
  }

}