package inventory.storage

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import inventory.events.Event
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future


case class EventTx(txId: Long, entityId: Long, event: Event)


trait EventStore {

  implicit val system = ActorSystem("event-store")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val BUFFER_SIZE = 1000

  val (actorRef, publisher) =  Source.actorRef[EventTx](BUFFER_SIZE, OverflowStrategy.fail).toMat(Sink.publisher)(Keep.both).run()

  val source = {
    Source(publisher).map{ eventTx =>
      Logger.debug("event passing through " + eventTx)
      eventTx
    }
  }

  def saveEvent(event: Event, entityId: Long = -1): Future[(Long, Long)] = {
    val eIdFuture = if (entityId <= 0) nextEntityId else Future.successful(entityId)

    val txIdeId = for {
      eId <- eIdFuture
      txId <- storeEvent(eId, event)
    } yield (txId, eId)

    txIdeId.onSuccess { case (txId, eId) =>
      Logger.debug("publishing event " + event)
      actorRef ! EventTx(txId, eId, event)
    }

    txIdeId
  }
  def nextEntityId: Future[Long]

  def storeEvent(entityId: Long, event: Event): Future[Long]

  def getEvents(entityId: Long): Future[Seq[Event]]
}