package inventory.domain

import inventory.events.{ProductSold, ProductCreated}
import inventory.storage.TestEventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.{WithApplication, FakeApplication, PlaySpecification}

class AggregateRootSpec extends PlaySpecification {

  "AggregateRoot" should {
    "get an entity by id and use the correct type through type class implicit" in {
      import ProductHelper.productEvents

      val create = ProductCreated("test", None, 5, None, 2.0, None)
      val eventStore = new TestEventStore

      val (product, eId) = await(for {
        (txId, eId) <- eventStore saveEvent create
        _ <- eventStore.saveEvent(ProductSold(eId, 2), eId)
        product <- AggregateRoot.getById(eId)(eventStore)
      } yield (product, eId))

      product must beEqualTo(Product(eId, "test", 3))
    }
  }
}
