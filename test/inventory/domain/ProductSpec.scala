package inventory.domain

import inventory.domain.Product.ProductAggregate
import inventory.events.{RestockProduct, SellProduct}
import play.api.test.PlaySpecification

class ProductSpec extends PlaySpecification {
  "Product" should {
    "reduce its quantity when its sold" in {
      val product = Product(1l, "test product", 3)
      val sell = SellProduct(1l, 2)

      val soldProduct = ProductAggregate(sell)(product)

      soldProduct must beASuccessfulTry(Product(1l, "test product", 1))
    }

    "increase it's quantity when its restocked" in {
      val product = Product(1l, "test product", 3)
      val restock = RestockProduct(1l, 2)

      val newStockProduct = ProductAggregate(restock)(product)

      newStockProduct must beASuccessfulTry(Product(1l, "test product", 5))
    }

    "not sell if the quantity is lower than sold, and create a notification that its failed" in {
      val product = Product(1l, "test product", 3)
      val sell = SellProduct(1l, 2)

      val soldProduct = ProductAggregate(sell)(product)

      soldProduct must beASuccessfulTry(Product(1l, "test product", 3))

    }
  }

}
