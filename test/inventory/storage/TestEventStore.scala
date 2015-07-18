package inventory.storage

import inventory.events.Event

import scala.concurrent.Future

/**
 * naive implementation of ES, not threadsafe
 */
class TestEventStore extends EventStore {
  var entityId = 0l
  var txId = 0l

  val events = collection.mutable.ListBuffer[(Long, Long, Event)]()

  override def nextEntityId: Future[Long] = {
    entityId += 1
    Future.successful(entityId)
  }

  override def getEvents(entityId: Long): Future[Seq[Event]] = {
    Future.successful{
      events.filter(_._2 == entityId).map{
        case (txId, entityId, event) => event
      }
    }
  }

  override def storeEvent(entityId: Long, event: Event): Future[Long] = {
    txId += 1

    events append ((txId, entityId, event))
    Future.successful(txId)
  }
}
