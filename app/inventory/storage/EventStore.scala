package inventory.storage

import inventory.events.Event

object EventStore {

  case class Data(txId: Long, entityId: Long, event: Event)

  val storage = collection.mutable.ListBuffer[Data]()

  var transactionId = 1l // this is really naive, not concurrent safe
  var entityId = 1l

  def nextTxId = {
    transactionId = transactionId + 1
    transactionId
  }

  def nextEntityId = {
    entityId = entityId + 1
    entityId
  }

  def saveEvent(event: Event, entityId: Long = -1): (Long, Long) = {
    val txId = nextTxId
    val eId = if (entityId <= 0) nextEntityId else entityId
    storage.append(Data(txId, eId, event))
    (txId, eId)
  }

  def loadEventsFor(entityId: Long): Seq[Event] = {
    storage.filter(_.entityId == entityId).map(_.event)
  }

}
