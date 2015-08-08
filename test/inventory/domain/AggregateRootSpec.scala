package inventory.domain

import java.util.UUID

import inventory.events.{ItemSold, ItemCreated}
import inventory.storage.TestEventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.{WithApplication, FakeApplication, PlaySpecification}

class AggregateRootSpec extends PlaySpecification {

  "AggregateRoot" should {
    "get an entity by id and use the correct type through type class implicit" in {
      import ItemHelper.itemEventHandler

      val id = UUID.randomUUID()
      val create = ItemCreated(id, "test", None, 5, None, 2.0, None)
      val eventStore = new TestEventStore

      val item = await(for {
        txId <- eventStore saveEvent (create, id)
        _ <- eventStore.saveEvent(ItemSold(id, 2), id)
        item <- AggregateRoot.getById(id)(eventStore)
      } yield item)

      item must beEqualTo(Item(id, "test", 3))
    }
  }
}
