package inventory.domain

import java.util.UUID

import inventory.commands.{Command, CommandHandler}
import inventory.events.{Event, FailedToApply}
import inventory.storage.EventStore
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

trait Entity {
  def id: UUID
}

trait EventHandler[T <: Entity] {
  def apply(event: Event)(entity: Option[T]): Try[T]
}


trait AggregateRoot {

  implicit val ec: ExecutionContext

  def loadFromHistory[T <: Entity : EventHandler](history: Iterable[Event]): Try[T] = {
    val applyEvent = implicitly[EventHandler[T]]
    history.headOption match {
      case Some(firstEvent) =>
        val initialEntity = applyEvent(firstEvent)(None)
        history.tail.foldLeft(initialEntity)((entity, event) => entity.flatMap(e =>applyEvent(event)(Some(e))))
      case None => Failure(new RuntimeException("empty history, nothing to load"))
    }
  }

  def tryTo[E <: Entity : CommandHandler, C <: Command](command: C)(entityOpt: Option[E]) = {
    new ApplyCommand(command, entityOpt)
  }

  def getById[T <: Entity : EventHandler](entityId: UUID)(eventStore: EventStore): Future[T] = {
    eventStore.getEvents(entityId).flatMap{ event =>
      Future fromTry loadFromHistory(event)
    }
  }

}

class ApplyCommand[E <: Entity : CommandHandler, C <: Command](command: C, entityOpt: Option[E]) {
  val commandHandler = implicitly[CommandHandler[E]]

  def doCommand(implicit ec: ExecutionContext) = Future.fromTry(commandHandler(command)(entityOpt)).map{
    case event => (entityOpt.map(_.id).getOrElse(UUID.randomUUID()), event)
  }

  def or(recover: (C, Option[E]) => Event)(implicit ec: ExecutionContext) = {
    doCommand.recover {
      case FailedToApply(command: C) =>
        Logger.debug(s"recovering from failed $command on $entityOpt")
        (UUID.randomUUID(), recover(command, entityOpt))
    }
  }
}