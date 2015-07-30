package inventory.domain

import inventory.events.{FailedToApply, Event}
import inventory.storage.EventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Success, Failure, Try}


trait EventApply[T] extends PartialFunction[(Event, Option[T]), T] {
  def apply(ep: (Event, Option[T])): T // TODO: should this be Try[T]
}


object AggregateRoot {

  def apply[T](event: Event)(entity: Option[T])(eventApply: EventApply[T]): Try[T] = {
    if (eventApply.isDefinedAt((event, entity)))
      Success(eventApply((event, entity)))
    else
      Failure(new FailedToApply(event))
  }

  def loadFromHistory[T : EventApply](history: Iterable[Event]): Try[T] = {
    val applyEvent = implicitly[EventApply[T]]
    history.headOption match {
      case Some(firstEvent) =>
        val initialEntity = applyEvent(firstEvent, None)
        Success(history.tail.foldLeft(initialEntity)((entity, event) => applyEvent(event, Some(entity))))
      case None => Failure(new RuntimeException("empty history, nothing to load"))
    }
  }

  def getById[T : EventApply](entityId: Long)(eventStore: EventStore): Future[T] = {
    eventStore.getEvents(entityId).flatMap{ event =>
      Future fromTry loadFromHistory(event)
    }
  }

}
