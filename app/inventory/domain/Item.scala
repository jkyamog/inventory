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
    case (event: ItemReduced, Some(item)) if event.quantity <= item.quantity =>
      Success(item.copy(quantity = item.quantity - event.quantity))
    case (event: ItemIncreased, Some(item)) =>
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
    case (ReduceItem(id, quantity), Some(item)) if id == item.id && quantity <= item.quantity =>
      Success(ItemReduced(item.id, quantity))
    case (ArchiveItem(id), Some(item)) if id == item.id =>
      Success(ItemArchived(item.id))
    case _ =>
      Logger.error(s"failed to apply $command on $itemOpt")
      Failure(new FailedToApply(command))

  }
}

object ItemHelper {
  implicit val itemEventHandler = new ItemEventHandler

  implicit val itemCommandHandler = new ItemCommandHandler

  val notifySellFailed = (sellItem: ReduceItem, entityOpt: Option[Item]) =>
    SellFailedNotification(entityOpt.map(_.id).getOrElse(UUID.randomUUID), sellItem.quantity)
}