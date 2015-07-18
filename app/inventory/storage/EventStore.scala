package inventory.storage

import inventory.events.Event

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

trait EventStore {

  def saveEvent(event: Event, entityId: Long = -1): Future[(Long, Long)] = {
    val eIdFuture = if (entityId <= 0) nextEntityId else Future.successful(entityId)

    for {
      eId <- eIdFuture
      txId <- storeEvent(eId, event)
    } yield (txId, eId)

  }

  def nextEntityId: Future[Long]

  def storeEvent(entityId: Long, event: Event): Future[Long]

  def getEvents(entityId: Long): Future[Seq[Event]]
}