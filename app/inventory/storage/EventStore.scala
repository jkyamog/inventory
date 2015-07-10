package inventory.storage

import inventory.events.{CreateProduct, Event}

import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, JsResult, Format, JsValue}

import slick.driver.JdbcProfile

import scala.concurrent.Future

object EventStore extends HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

  var entityIdSequence = Sequence[Long]("events_entity_id_seq")

  def nextEntityId = db.run(
    entityIdSequence.next.result
  )

  case class EventData(txId: Option[Long], entityId: Long, Event: String)

  class Events(tag: Tag) extends Table[EventData](tag, "events") {

    def txId = column[Long]("tx_id", O.PrimaryKey, O.AutoInc)
    def entityId = column[Long]("entity_id")
    def event = column[String]("event")

    def * = (txId.?, entityId, event) <> (EventData.tupled, EventData.unapply _)
  }

  val events = TableQuery[Events]

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = {
      val eventType = (js \ "type").as[String]
      eventType match {
        case "CreateProduct" =>
          implicit val productCreateFormatter = Json.format[CreateProduct]

          val event = (js \ "event").get
          Json.fromJson[CreateProduct](event)
      }
    }


    def writes(event: Event): JsValue = {
      event match {
        case c: CreateProduct =>
          implicit val productCreateFormatter = Json.format[CreateProduct]

          val json = Json.toJson(c)

          Json.obj("type" -> "CreateProduct", "event" -> json)
      }
    }
  }

  def saveEvent(event: Event, entityId: Long = -1): Future[(Long, Long)] = {
    val eIdFuture = if (entityId <= 0) nextEntityId else Future.successful(entityId)
    val eventJson = Json.toJson(event)

    eIdFuture.flatMap { eId =>
      val eventData = EventData(None, eId, eventJson.toString)

      db.run {
        for {
          txId <- events.returning(events.map(_.txId)) += eventData
        } yield (txId, eId)
      }
    }

  }

  def loadEventsFor(entityId: Long): Future[Seq[Event]] = {
    val eventsOnDb = db.run(
      events.filter(_.entityId === entityId).map(_.event).result
    )

    eventsOnDb.map { eventStr =>
      val jsons = eventStr.map(Json.parse)
      jsons.map{ json =>
        Json.fromJson[Event](json).get
      }
    }
  }

}


