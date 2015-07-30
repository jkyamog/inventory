package inventory.storage

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import inventory.events.Event
import org.reactivestreams.{Subscription, Subscriber}

import play.api.{Logger, Play}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile


object SqlEventStore extends EventStore with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._
  import JsonFormatter.eventFormatter

  case class EventData(txId: Option[Long], entityId: Long, event: String)

  class Events(tag: Tag) extends Table[EventData](tag, "events") {

    def txId = column[Long]("tx_id", O.PrimaryKey, O.AutoInc)
    def entityId = column[Long]("entity_id")
    def event = column[String]("event")

    def * = (txId.?, entityId, event) <> (EventData.tupled, EventData.unapply _)
  }

  val events = TableQuery[Events]

  var entityIdSequence = Sequence[Long]("events_entity_id_seq")

  def nextEntityId = db.run(
    entityIdSequence.next.result
  )

  implicit val system = ActorSystem("Sys")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val (actorRef, publisher) =  Source.actorRef[String](1000, OverflowStrategy.fail).toMat(Sink.publisher)(Keep.both).run()

  Source(publisher).map { str =>
    Logger.debug("echo " + str)
  }.to(Sink.foreach(str => Logger.debug("here" + str))).run()

  def storeEvent(entityId: Long, event: Event) = {
    val eventJson = Json.toJson(event)
    val eventData = EventData(None, entityId, eventJson.toString)

    val id = db.run(
      events.returning(events.map(_.txId)) += eventData
    )

    id.onSuccess{
      case _ => actorRef ! eventJson.toString
    }

    id
  }

  def getEvents(entityId: Long) = {
    val eventsFromDb = db.run(
      events.sortBy(_.txId).filter(_.entityId === entityId).result
    )

    eventsFromDb.map { events =>
      val jsons = events.map(e => Json.parse(e.event))
      jsons.map{ json =>
        Json.fromJson[Event](json).get
      }
    }

  }

  def allEvents = {
    val eventsFromDb = db.stream(
      events.sortBy(_.txId).result
    )

    eventsFromDb.mapResult { eventData =>
      Json.parse(eventData.event)
    }

//    eventsFromDb.map { events =>
//      val jsons = events.map(e => Json.parse(e.event))
//      jsons.map{ json =>
//        Json.fromJson[Event](json).get
//      }
//    }

  }

}