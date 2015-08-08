package inventory.events

import java.util.UUID

sealed trait Event

case class ItemSold(
  id: UUID,
  quantity: Int
) extends Event

case class ItemRestocked(
  id: UUID,
  quantity: Int
) extends Event

case class ItemCreated(
  id: UUID,
  name: String,
  description: Option[String],
  quantity: Int,
  reorderPoint: Option[Int],
  price: BigDecimal,
  packaging: Option[String]
) extends Event

case class ItemArchived(
  id: UUID
) extends Event

case class ReorderNotification(
  id: UUID
) extends Event

case class DismissNotification(
  id: UUID
) extends Event

case class SellFailedNotification(
  id: UUID,
  quantityAttempted: Int
) extends Event

class FailedToApply[T](val t: T) extends RuntimeException

object FailedToApply {
  def unapply[T](fta: FailedToApply[T]): Option[T] = {
    Some(fta.t)
  }
}