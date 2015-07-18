package inventory.domain

import inventory.events.{CreateProduct, SellProduct, Event}
import play.api.Logger

import scala.util.{Failure, Success, Try}


case class Product(id: Long, name: String, quantity: Int) {
  def sell(quantity: Int) = this.copy(quantity = this.quantity - quantity)
}

object Product {
  implicit object ProductAggregate extends AggregateRoot[Product] {
    override def apply(event: Event)(product: Product): Try[Product] = event match {
      case SellProduct(productId, quantity) if productId == product.id =>
        Success(product.sell(quantity))
    }

    override def init(entityId: Long, event: Event): Try[Product] = event match {
      case CreateProduct(name, description, quantity, reorderPoint, price, packaging) =>
        Success(Product(entityId, name, quantity))
      case _ =>
        val message = "Not possible to create project from event: " + event
        Logger.error(message)
        Failure(new RuntimeException(message))
    }
  }

}