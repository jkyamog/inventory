package inventory.domain

import java.util.UUID

import inventory.commands._
import inventory.events._
import play.api.Logger

case class Product(id: UUID, name: String, quantity: Int)

class ProductEvents extends EventApply[Product] {
  override def apply(ep: (Event, Option[Product])) = ep match {
    case (ProductCreated(productId, name, description, quantity, reorderPoint, price, packaging), None) =>
      Product(productId, name, quantity)
    case (event: ProductSold, Some(product)) if event.quantity <= product.quantity =>
      product.copy(quantity = product.quantity - event.quantity)
  }

  override def isDefinedAt(ep: (Event, Option[Product])): Boolean = ep match {
    case (ProductCreated(productId, name, description, quantity, reorderPoint, price, packaging), None) =>
      true
    case (event: ProductSold, Some(product)) if event.quantity <= product.quantity =>
      true
    case _ => false
  }
}

object ProductCommand extends CommandApply[Product] {
  override def apply(command: Command)(product: Option[Product]) = (command, product) match {
    case (CreateProduct(name, description, quantity, reorderPoint, price, packaging), None) =>
      ProductCreated(UUID.randomUUID(), name, description, quantity, reorderPoint, price, packaging)
    case (SellProduct(productId, quantity), Some(product)) if quantity <= product.quantity =>
      ProductSold(productId, quantity)
  }
}

object ProductHelper {
  implicit val productEvents = new ProductEvents
}