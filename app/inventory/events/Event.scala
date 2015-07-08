package inventory.events

sealed trait Event {

}

case class SellProduct(
  productId: Long,
  quantity: Int
) extends Event
// TODO validate that quantity is not below 0 after selling

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

case class NotifyReorder(
  productId: Long
) extends Event

case class DismissNotification(
  notificationId: Long
) extends Event