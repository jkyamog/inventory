package inventory.storage

import inventory.events.Event

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import scala.concurrent.Future

case class EventData(txId: Option[Long], entityId: Long, event: String)

trait EventStore {

  def nextEntityId: Future[Long]

  def storeEvent(eventData: EventData): Future[Long]

  def getEvents(entityId: Long): Future[Seq[EventData]]
}


object EventStore extends SqlEventStore {

  import JsonFormatter.eventFormatter

  def saveEvent(event: Event, entityId: Long = -1): Future[(Long, Long)] = {
    val eIdFuture = if (entityId <= 0) nextEntityId else Future.successful(entityId)
    val eventJson = Json.toJson(event)

    eIdFuture.flatMap { eId =>
      val eventData = EventData(None, eId, eventJson.toString)

        for {
          txId <- storeEvent(eventData)
        } yield (txId, eId)
    }

  }

  def loadEventsFor(entityId: Long): Future[Seq[Event]] = {
    getEvents(entityId).map { events =>
      val jsons = events.map(e => Json.parse(e.event))
      jsons.map{ json =>
        Json.fromJson[Event](json).get
      }
    }
  }

}


