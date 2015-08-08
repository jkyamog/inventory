package inventory.domain

import java.util.UUID

import inventory.commands._
import inventory.events._

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}


case class Product(id: UUID, name: String, quantity: Int)

class ProductEvents extends EventApply[Product] {
  override def apply(event: Event)(entity: Option[Product]) = (event, entity) match {
    case (ProductCreated(productId, name, description, quantity, reorderPoint, price, packaging), None) =>
      Success(Product(productId, name, quantity))
    case (event: ProductSold, Some(product)) if event.quantity <= product.quantity =>
      Success(product.copy(quantity = product.quantity - event.quantity))
    case _ =>
      Failure(new FailedToApply(event))
  }
}

object ProductCommand extends CommandApply[Product] {
  override def apply(command: Command)(product: Option[Product]) = (command, product) match {
    case (CreateProduct(name, description, quantity, reorderPoint, price, packaging), None) =>
      val event = ProductCreated(UUID.randomUUID(), name, description, quantity, reorderPoint, price, packaging)
      Logger.debug("event created " + event)
      Success(event)
    case (SellProduct(productId, quantity), Some(product)) if quantity <= product.quantity =>
      Success(ProductSold(productId, quantity))
    case _ =>
      Logger.error("failed to apply " + command)
      Failure(new FailedToApply[Command](command))

  }
}

object ProductHelper {
  implicit val productEvents = new ProductEvents

  def tryTo: SellProduct => Some[Product] => Future[(UUID, Event)] = { sellProduct => someProduct => //{ sellProduct: SellProduct => someProduct: Some[Product] =>

    Future.fromTry(ProductCommand(sellProduct)(someProduct)).map{
      case soldProduct => (someProduct.get.id, soldProduct)
    }.recover{
      case FailedToApply(command: SellProduct) =>
        Logger.debug("recovering from failed " + command)
        (UUID.randomUUID(), SellFailedNotification(someProduct.get.id, sellProduct.quantity))
    }
  }


}