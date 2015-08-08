package inventory.storage

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import inventory.events.Event
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future


case class EventTx(txId: Long, entityId: UUID, event: Event)


trait EventStore {

  implicit val system = ActorSystem("event-store")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val BUFFER_SIZE = 1000

  val (actorRef, publisher) =  Source.actorRef[EventTx](BUFFER_SIZE, OverflowStrategy.fail).toMat(Sink.publisher)(Keep.both).run()

  val source = Source(publisher)

  def saveEvent(event: Event, entityId: UUID): Future[Long] = {
    val txId = for {
      txId <- storeEvent(entityId, event)
    } yield txId

    txId.onSuccess { case txId =>
      Logger.debug("publishing event " + event)
      actorRef ! EventTx(txId, entityId, event)
    }

    txId
  }
  def storeEvent(entityId: UUID, event: Event): Future[Long]

  def getEvents(entityId: UUID): Future[Seq[Event]]
}