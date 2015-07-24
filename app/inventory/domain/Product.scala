package inventory.domain

import inventory.events.{FailedToApply, CreateProduct, SellProduct, Event}
import play.api.Logger

import scala.util.{Failure, Success, Try}

case class Product(id: Long, name: String, quantity: Int)

class ProductEvents extends PartialFunction[(Event, Option[Product]), Product] {
  override def apply(ep: (Event, Option[Product])) = ep match {
    case (CreateProduct(name, description, quantity, reorderPoint, price, packaging), None) =>
      Product(-1, name, quantity)
    case (event: SellProduct, Some(product)) if event.quantity <= product.quantity =>
      product.copy(quantity = product.quantity - event.quantity)
  }

  override def isDefinedAt(ep: (Event, Option[Product])): Boolean = ep match {
    case (event: SellProduct, Some(product)) if event.quantity <= product.quantity =>
      true
    case _ => false
  }
}

object Product {
  implicit object ProductAggregate extends AggregateRoot[Product] {
    override def apply(event: Event)(product: Option[Product]): Try[Product] = {
      val pe = new ProductEvents
      if (pe.isDefinedAt((event, product)))
        Success(pe((event, product)))
      else
        Failure(new FailedToApply(event))
    }
  }
}