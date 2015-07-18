package inventory.domain

import inventory.events.Event
import inventory.storage.EventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future


trait AggregateRoot[T] {

  def apply(entity: T, event: Event): T

  def init(entityId: Long, event: Event): Option[T]

}

object AggregateRoot {
  def loadFromHistory[T : AggregateRoot](entityId: Long, history: Seq[Event]): Option[T] = {
    val aggregateRoot = implicitly[AggregateRoot[T]]
    history.headOption.flatMap { firstEvent =>
      aggregateRoot.init(entityId, firstEvent).map { initialEntity =>
        history.tail.foldLeft(initialEntity)((entity, event) => aggregateRoot.apply(entity, event))
      }
    }
  }

  def getById[T : AggregateRoot](entityId: Long)(eventStore: EventStore): Future[Option[T]] = {
    eventStore.getEvents(entityId).map { events =>
      loadFromHistory(entityId, events)
    }
  }

}
