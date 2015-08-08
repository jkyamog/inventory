package inventory.domain

import java.util.UUID

import inventory.commands._
import inventory.events._

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}


case class Item(id: UUID, name: String, quantity: Int)

class ItemEventHandler extends EventHandler[Item] {
  override def apply(event: Event)(entity: Option[Item]) = (event, entity) match {
    case (ItemCreated(id, name, description, quantity, reorderPoint, price, packaging), None) =>
      Success(Item(id, name, quantity))
    case (event: ItemSold, Some(item)) if event.quantity <= item.quantity =>
      Success(item.copy(quantity = item.quantity - event.quantity))
    case _ =>
      Failure(new FailedToApply(event))
  }
}

object ItemCommandHandler extends CommandHandler[Item] {
  override def apply(command: Command)(itemOpt: Option[Item]) = (command, itemOpt) match {
    case (CreateItem(name, description, quantity, reorderPoint, price, packaging), None) =>
      val event = ItemCreated(UUID.randomUUID(), name, description, quantity, reorderPoint, price, packaging)
      Logger.debug("event created " + event)
      Success(event)
    case (SellItem(id, quantity), Some(item)) if quantity <= item.quantity =>
      Success(ItemSold(id, quantity))
    case _ =>
      Logger.error("failed to apply " + command)
      Failure(new FailedToApply(command))

  }
}

object ItemHelper {
  implicit val itemEventHandler = new ItemEventHandler

  def tryTo: SellItem => Some[Item] => Future[(UUID, Event)] = { sellItem => someItem =>

    Future.fromTry(ItemCommandHandler(sellItem)(someItem)).map{
      case soldItem => (someItem.get.id, soldItem)
    }.recover{
      case FailedToApply(command: SellItem) =>
        Logger.debug("recovering from failed " + command)
        (UUID.randomUUID(), SellFailedNotification(someItem.get.id, sellItem.quantity))
    }
  }


}