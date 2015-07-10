package inventory.domain

import inventory.events.{SellProduct, CreateProduct}
import inventory.storage.EventStore

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.PlaySpecification

class AggregateRootSpec extends PlaySpecification {

  "AggregateRoot" should {
    "get an entity by id and use the correct type through type class implicit" in {
      import Product.ProductAggregate

      val create = CreateProduct("test", None, 5, None, 2.0, None)

      val (product, eId) = await(for {
        (txId, eId) <- EventStore saveEvent create
        _ <- EventStore.saveEvent(SellProduct(eId, 2), eId)
        product <- AggregateRoot.getById(eId)
      } yield (product, eId))

      product must beSome(Product(eId, "test", 3))
    }
  }
}
