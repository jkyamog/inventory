package inventory.domain

import java.util.UUID

import inventory.commands.SellProduct
import inventory.domain.ProductHelper.productEvents
import inventory.events._
import play.api.test.PlaySpecification

class ProductSpec extends PlaySpecification {
  "Product" should {
    "reduce its quantity when its sold" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 3)
      val sell = ProductSold(id, 2)

      val soldProduct = productEvents(sell)(Some(product))

      soldProduct must beSuccessfulTry(Product(id, "test product", 1))
    }

    "increase it's quantity when its restocked" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 3)
      val restock = ProductRestocked(id, 2)

      val newStockProduct = productEvents(restock)(Some(product))

      newStockProduct must beSuccessfulTry(Product(id, "test product", 5))
    }

    "not sell if the quantity is lower than sold and fail" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 2)
      val sell = ProductSold(id, 3)

      val soldProduct = productEvents(sell)(Some(product))

      soldProduct must beFailedTry
    }

    "when not sold should produce a sell failed notification event" in {
      val id = UUID.randomUUID()
      val product = Product(id, "test product", 2)
      val sell = SellProduct(id, 3)

      val (_, event) = await(ProductHelper.tryTo(sell)(Some(product)))

      event must beLike{ case SellFailedNotification(_, quantity) => quantity must beEqualTo(3) }
    }
  }

}
