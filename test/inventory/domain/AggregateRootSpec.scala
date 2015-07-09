package inventory.domain

import inventory.events.{SellProduct, CreateProduct}
import inventory.storage.EventStore
import org.specs2.mutable.Specification


class AggregateRootSpec extends Specification {

  "AggregateRoot" should {
    "get an entity by id and use the correct type through type class implicit" in {
      import Product.ProductAggregate

      val create = CreateProduct("test", None, 5, None, 2.0, None)

      val (txId, eId) = EventStore saveEvent create
      EventStore.saveEvent(SellProduct(eId, 2), eId)

      val product = AggregateRoot.getById(eId)

      product must beSome(Product(eId, "test", 3))
    }
  }
}
