package inventory.storage

import java.util.UUID

import inventory.events.Event

import scala.concurrent.Future

/**
 * naive implementation of ES, not threadsafe
 */
class TestEventStore extends EventStore {
  var txId = 0l

  val events = collection.mutable.ListBuffer[(Long, UUID, Event)]()

  override def getEvents(entityId: UUID): Future[Seq[Event]] = {
    Future.successful{
      events.filter(_._2 == entityId).map{
        case (txId, entityId, event) => event
      }
    }
  }

  override def storeEvent(entityId: UUID, event: Event): Future[Long] = {
    txId += 1

    events append ((txId, entityId, event))
    Future.successful(txId)
  }
}
