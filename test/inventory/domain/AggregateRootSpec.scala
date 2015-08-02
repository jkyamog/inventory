package inventory.domain

import java.util.UUID

import inventory.events.{ProductSold, ProductCreated}
import inventory.storage.TestEventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.{WithApplication, FakeApplication, PlaySpecification}

class AggregateRootSpec extends PlaySpecification {

  "AggregateRoot" should {
    "get an entity by id and use the correct type through type class implicit" in {
      import ProductHelper.productEvents

      val id = UUID.randomUUID()
      val create = ProductCreated(id, "test", None, 5, None, 2.0, None)
      val eventStore = new TestEventStore

      val product = await(for {
        txId <- eventStore saveEvent (create, id)
        _ <- eventStore.saveEvent(ProductSold(id, 2), id)
        product <- AggregateRoot.getById(id)(eventStore)
      } yield product)

      product must beEqualTo(Product(id, "test", 3))
    }
  }
}
