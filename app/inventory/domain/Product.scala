package inventory.domain

import inventory.events.{FailedToApply, CreateProduct, SellProduct, Event}
import play.api.Logger

import scala.util.{Failure, Success, Try}

case class Product(id: Long, name: String, quantity: Int)

class ProductEvents extends PartialFunction[(Event, Product), Product] {
  override def apply(ep: (Event, Product)) = ep match {
    case (event: SellProduct, product: Product) if event.quantity <= product.quantity =>
      product.copy(quantity = product.quantity - event.quantity)
  }

  override def isDefinedAt(ep: (Event, Product)): Boolean = ep match {
    case (event: SellProduct, product: Product) if event.quantity <= product.quantity =>
      true
    case _ => false
  }
}

object Product {
  implicit object ProductAggregate extends AggregateRoot[Product] {
    override def apply(event: Event)(product: Product): Try[Product] = {
      val pe = new ProductEvents
      if (pe.isDefinedAt((event, product)))
        Success(pe((event, product)))
      else
        Failure(new FailedToApply(event))
    }

    override def init(entityId: Long, event: Event): Try[Product] = event match {
      case CreateProduct(name, description, quantity, reorderPoint, price, packaging) =>
        Success(Product(entityId, name, quantity))
      case _ =>
        val message = "Not possible to init product from event: " + event
        Logger.error(message)
        Failure(new RuntimeException(message))
    }
  }

}