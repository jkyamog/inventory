package inventory.commands

import inventory.events.Event

sealed trait Command

case class SellProduct(
  productId: Long,
  quantity: Int
) extends Command

case class RestockProduct(
  productId: Long,
  quantity: Int
) extends Command

case class CreateProduct(
  name: String,
  description: Option[String],
  quantity: Int,
  reorderPoint: Option[Int],
  price: BigDecimal,
  packaging: Option[String]
) extends Command

case class ArchiveProduct(
  productId: Long
) extends Command

trait CommandApply[T] {
  def apply(command: Command)(entity: Option[T]): Event
}