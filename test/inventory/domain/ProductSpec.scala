package inventory.domain

import java.util.UUID

import inventory.domain.ProductHelper.productEvents
import inventory.events.{ProductRestocked, ProductSold}
import play.api.test.PlaySpecification

class ProductSpec extends PlaySpecification {
  "Product" should {
    "reduce its quantity when its sold" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 3)
      val sell = ProductSold(id, 2)

      val soldProduct = Some((sell, Some(product))) collect productEvents

      soldProduct must beSome(Product(id, "test product", 1))
    }

    "increase it's quantity when its restocked" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 3)
      val restock = ProductRestocked(id, 2)

      val newStockProduct = Some((restock, Some(product))) collect productEvents

      newStockProduct must beSome(Product(id, "test product", 5))
    }

    "not sell if the quantity is lower than sold" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 2)
      val sell = ProductSold(id, 3)

      val soldProduct = Some((sell, Some(product))) collect productEvents

      soldProduct must beNone//(FailedToSell)
//        case FailedToSell(event) => event must beEqualTo(sell)
//      }//beASuccessfulTry(Product(1l, "test product", 3))

    }
  }

}
