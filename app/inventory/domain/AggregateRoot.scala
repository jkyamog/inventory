package inventory.domain

import inventory.events.Event
import inventory.storage.EventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Try}


trait AggregateRoot[T] {

  def apply(event: Event)(entity: T): Try[T]

  def init(entityId: Long, event: Event): Try[T]

}

object AggregateRoot {
  def loadFromHistory[T : AggregateRoot](entityId: Long, history: Seq[Event]): Try[T] = {
    val aggregateRoot = implicitly[AggregateRoot[T]]
    history.headOption match {
      case Some(firstEvent) =>
        val initialEntity = aggregateRoot.init(entityId, firstEvent)
        history.tail.foldLeft(initialEntity)((entity, event) => entity.flatMap(aggregateRoot(event)))
      case None => Failure(new RuntimeException("empty history, nothing to load for entityId: " + entityId))
    }
  }

  def getById[T : AggregateRoot](entityId: Long)(eventStore: EventStore): Future[Try[T]] = {
    eventStore.getEvents(entityId).map { events =>
      loadFromHistory(entityId, events)
    }
  }

}
