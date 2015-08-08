package inventory.events

import java.util.UUID

sealed trait Event

case class ProductSold(
  productId: UUID,
  quantity: Int
) extends Event

case class ProductRestocked(
  productId: UUID,
  quantity: Int
) extends Event

case class ProductCreated(
  productId: UUID,
  name: String,
  description: Option[String],
  quantity: Int,
  reorderPoint: Option[Int],
  price: BigDecimal,
  packaging: Option[String]
) extends Event

case class ProductArchived(
  productId: UUID
) extends Event

case class ReorderNotification(
  productId: Long
) extends Event

case class DismissNotification(
  notificationId: Long
) extends Event

case class SellFailedNotification(
  productId: UUID,
  quantityAttempted: Int
) extends Event

class FailedToApply[T](val t: T) extends RuntimeException

object FailedToApply {
  def unapply[T](fta: FailedToApply[T]): Option[T] = {
    Some(fta.t)
  }
}