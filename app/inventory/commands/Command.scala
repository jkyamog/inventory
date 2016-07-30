package inventory.commands

import java.util.UUID

import inventory.domain.Entity
import inventory.events.Event

import scala.util.Try

sealed trait Command

case class ReduceItem(
  id: UUID,
  quantity: Int
) extends Command

case class IncreaseItem(
  id: UUID,
  quantity: Int
) extends Command

case class CreateItem(
  name: String,
  description: Option[String],
  quantity: Int,
  reorderPoint: Option[Int],
  price: BigDecimal,
  packaging: Option[String]
) extends Command

case class ArchiveItem(
  id: UUID
) extends Command

trait CommandHandler[T <: Entity] {
  def apply(command: Command)(entity: Option[T]): Try[Event]
}