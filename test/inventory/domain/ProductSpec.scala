package inventory.domain

import inventory.domain.ProductHelper.productEvents
import inventory.events.{ProductRestocked, ProductSold}
import play.api.test.PlaySpecification

class ProductSpec extends PlaySpecification {
  "Product" should {
    "reduce its quantity when its sold" in {
      val product = Product(1l, "test product", 3)
      val sell = ProductSold(1l, 2)

      val soldProduct = Some((sell, Some(product))) collect productEvents

      soldProduct must beSome(Product(1l, "test product", 1))
    }

    "increase it's quantity when its restocked" in {
      val product = Product(1l, "test product", 3)
      val restock = ProductRestocked(1l, 2)

      val newStockProduct = Some((restock, Some(product))) collect productEvents

      newStockProduct must beSome(Product(1l, "test product", 5))
    }

    "not sell if the quantity is lower than sold" in {
      val product = Product(1l, "test product", 2)
      val sell = ProductSold(1l, 3)

      val soldProduct = Some((sell, Some(product))) collect productEvents

      soldProduct must beNone//(FailedToSell)
//        case FailedToSell(event) => event must beEqualTo(sell)
//      }//beASuccessfulTry(Product(1l, "test product", 3))

    }
  }

}
