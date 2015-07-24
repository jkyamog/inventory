package inventory.domain

import inventory.events.Event
import inventory.storage.EventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Try}


trait AggregateRoot[T] {

  def apply(event: Event)(entity: Option[T]): Try[T]

}

object AggregateRoot {
  def loadFromHistory[T : AggregateRoot](history: Iterable[Event]): Try[T] = {
    val aggregateRoot = implicitly[AggregateRoot[T]]
    history.headOption match {
      case Some(firstEvent) =>
        val initialEntity = aggregateRoot.apply(firstEvent)(None)
        history.tail.foldLeft(initialEntity)((entity, event) => entity.flatMap(e => aggregateRoot(event)(Some(e))))
      case None => Failure(new RuntimeException("empty history, nothing to load"))
    }
  }

  def getById[T : AggregateRoot](entityId: Long)(eventStore: EventStore): Future[T] = {
    eventStore.getEvents(entityId).flatMap{ event =>
      Future fromTry loadFromHistory(event)
    }
  }

}
