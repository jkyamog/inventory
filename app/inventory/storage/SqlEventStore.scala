package inventory.storage

import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}

import slick.driver.JdbcProfile

trait SqlEventStore extends EventStore with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

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

  def storeEvent(eventData: EventData) = {
    db.run(
      events.returning(events.map(_.txId)) += eventData
    )
  }

  def getEvents(entityId: Long) = {
    db.run(
      events.filter(_.entityId === entityId).result
    )
  }

}