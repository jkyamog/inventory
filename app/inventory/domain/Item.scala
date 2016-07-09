package inventory.domain

import java.util.UUID

import inventory.commands._
import inventory.events._

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}


case class Item(id: UUID, name: String, quantity: Int) extends Entity

class ItemEventHandler extends EventHandler[Item] {
  override def apply(event: Event)(entity: Option[Item]) = (event, entity) match {
    case (ItemCreated(id, name, description, quantity, reorderPoint, price, packaging, version), None) =>
      Success(Item(id, name, quantity))
    case (event: ItemSold, Some(item)) if event.quantity <= item.quantity =>
      Success(item.copy(quantity = item.quantity - event.quantity))
    case (event: ItemRestocked, Some(item)) =>
      Success(item.copy(quantity = item.quantity + event.quantity))
    case (event: ItemArchived, Some(item)) =>
      Success(item)
    case _ =>
      Failure(new FailedToApply(event))
  }
}

class ItemCommandHandler extends CommandHandler[Item] {
  override def apply(command: Command)(itemOpt: Option[Item]) = (command, itemOpt) match {
    case (CreateItem(name, description, quantity, reorderPoint, price, packaging), None) =>
      val event = ItemCreated(UUID.randomUUID(), name, description, quantity, reorderPoint, price, packaging)
      Logger.debug("event created " + event)
      Success(event)
    case (SellItem(quantity), Some(item)) if quantity <= item.quantity =>
      Success(ItemSold(item.id, quantity))
    case (ArchiveItem(), Some(item)) =>
      Success(ItemArchived(item.id))
    case _ =>
      Logger.error(s"failed to apply $command on $itemOpt")
      Failure(new FailedToApply(command))

  }
}

object ItemHelper {
  implicit val itemEventHandler = new ItemEventHandler

  implicit val itemCommandHandler = new ItemCommandHandler

  val notifySellFailed = (sellItem: SellItem, entityOpt: Option[Item]) =>
    SellFailedNotification(entityOpt.map(_.id).getOrElse(UUID.randomUUID), sellItem.quantity)
}