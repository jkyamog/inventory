package inventory.controllers

import inventory.commands._
import inventory.domain.Item
import inventory.events._
import play.api.libs.json.Json

object JsonHelpers {

  implicit val itemFormatter = Json.format[Item]

  implicit val createItemFormatter = Json.format[CreateItem]
  implicit val sellItemFormatter = Json.format[SellItem]

  implicit val itemCreatedFormatter = Json.format[ItemCreated]
  implicit val itemSoldFormatter = Json.format[ItemSold]
  implicit val sellFailedNotificationFormatter = Json.format[SellFailedNotification]

}
