package inventory.events

sealed trait Event

case class SellProduct(
  productId: Long,
  quantity: Int
) extends Event

case class RestockProduct(
  productId: Long,
  quantity: Int
) extends Event

case class CreateProduct(
  name: String,
  description: Option[String],
  quantity: Int,
  reorderPoint: Option[Int],
  price: BigDecimal,
  packaging: Option[String]
) extends Event

case class ArchiveProduct(
  productId: Long
) extends Event

case class ReorderNotification(
  productId: Long
) extends Event

case class DismissNotification(
  notificationId: Long
) extends Event

case class SellFailedNotification(
  productId: Long,
  quantityAttempted: Int
) extends Event

class FailedToApply(val event: Event) extends RuntimeException

object FailedToApply {
  def unapply(ue: FailedToApply): Option[Event] = {
    Some(ue.event)
  }
}