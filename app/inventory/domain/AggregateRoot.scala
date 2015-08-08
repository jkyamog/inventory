package inventory.domain

import java.util.UUID

import inventory.events.{FailedToApply, Event}
import inventory.storage.EventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Success, Failure, Try}


trait EventHandler[T] {
  def apply(event: Event)(entity: Option[T]): Try[T]
}


object AggregateRoot {

  def loadFromHistory[T : EventHandler](history: Iterable[Event]): Try[T] = {
    val applyEvent = implicitly[EventHandler[T]]
    history.headOption match {
      case Some(firstEvent) =>
        val initialEntity = applyEvent(firstEvent)(None)
        history.tail.foldLeft(initialEntity)((entity, event) => entity.flatMap(e =>applyEvent(event)(Some(e))))
      case None => Failure(new RuntimeException("empty history, nothing to load"))
    }
  }

  def getById[T : EventHandler](entityId: UUID)(eventStore: EventStore): Future[T] = {
    eventStore.getEvents(entityId).flatMap{ event =>
      Future fromTry loadFromHistory(event)
    }
  }

}
