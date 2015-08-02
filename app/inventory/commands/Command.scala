package inventory.commands

import java.util.UUID

import inventory.events.Event

sealed trait Command

case class SellProduct(
  productId: UUID,
  quantity: Int
) extends Command

case class RestockProduct(
  productId: UUID,
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
  productId: UUID
) extends Command

trait CommandApply[T] {
  def apply(command: Command)(entity: Option[T]): Event
}