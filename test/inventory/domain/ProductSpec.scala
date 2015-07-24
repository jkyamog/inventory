package inventory.domain

import inventory.domain.Product.ProductAggregate
import inventory.events.{RestockProduct, SellProduct}
import play.api.test.PlaySpecification

class ProductSpec extends PlaySpecification {
  "Product" should {
    "reduce its quantity when its sold" in {
      val product = Product(1l, "test product", 3)
      val sell = SellProduct(1l, 2)

      val soldProduct = ProductAggregate(sell)(Some(product))

      soldProduct must beASuccessfulTry(Product(1l, "test product", 1))
    }

    "increase it's quantity when its restocked" in {
      val product = Product(1l, "test product", 3)
      val restock = RestockProduct(1l, 2)

      val newStockProduct = ProductAggregate(restock)(Some(product))

      newStockProduct must beASuccessfulTry(Product(1l, "test product", 5))
    }

    "not sell if the quantity is lower than sold" in {
      val product = Product(1l, "test product", 2)
      val sell = SellProduct(1l, 3)

      val soldProduct = ProductAggregate(sell)(Some(product))

      soldProduct must beFailedTry//(FailedToSell)
//        case FailedToSell(event) => event must beEqualTo(sell)
//      }//beASuccessfulTry(Product(1l, "test product", 3))

    }
  }

}
