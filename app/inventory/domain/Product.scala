package inventory.domain

import inventory.events.{CreateProduct, SellProduct, Event}
import play.api.Logger


case class Product(id: Long, name: String, quantity: Int) {
  def sell(quantity: Int) = this.copy(quantity = this.quantity - quantity)
}

object Product {
  implicit object ProductAggregate extends AggregateRoot[Product] {
    override def apply(product: Product, event: Event): Product = event match {
      case SellProduct(productId, quantity) if productId == product.id =>
        product.sell(quantity)
    }

    override def init(entityId: Long, event: Event): Option[Product] = event match {
      case CreateProduct(name, description, quantity, reorderPoint, price, packaging) =>
        Some(Product(entityId, name, quantity))
      case _ =>
        Logger.error("Not possible to create project from event: " + event)
        None
    }
  }

}