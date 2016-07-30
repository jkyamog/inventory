package inventory.controllers

import inventory.commands._
import inventory.domain.Item
import inventory.events._
import play.api.libs.json.Json

object JsonHelpers {

  implicit val itemFormatter = Json.format[Item]

  implicit val createItemFormatter = Json.format[CreateItem]
  implicit val sellItemFormatter = Json.format[ReduceItem]

  implicit val itemCreatedFormatter = Json.format[ItemCreated]
  implicit val itemIncreasedFormatter = Json.format[ItemIncreased]
  implicit val itemReducedFormatter = Json.format[ItemReduced]
  implicit val sellFailedNotificationFormatter = Json.format[SellFailedNotification]
  implicit val itemArchivedFormatter = Json.format[ItemArchived]

}
