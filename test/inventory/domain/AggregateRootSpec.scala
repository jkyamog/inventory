package inventory.domain

import java.util.UUID

import inventory.events.{ItemCreated, ItemReduced}
import inventory.storage.TestEventStore
import play.api.test.PlaySpecification

import scala.concurrent.ExecutionContext

class AggregateRootSpec extends PlaySpecification {

  val ar = new AggregateRoot {
    override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  "AggregateRoot" should {
    "get an entity by id and use the correct type through type class implicit" in {
      import ItemHelper.itemEventHandler
      import scala.concurrent.ExecutionContext.Implicits.global

      val id = UUID.randomUUID()
      val create = ItemCreated(id, "test", None, 5, None, 2.0, None)
      val eventStore = new TestEventStore

      val item = await(for {
        txId <- eventStore saveEvent (create, id)
        _ <- eventStore.saveEvent(ItemReduced(id, 2), id)
        item <- ar.getById(id)(eventStore)
      } yield item)

      item must beEqualTo(Item(id, "test", 3))
    }
  }
}
