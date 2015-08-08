package inventory.controllers

import inventory.commands.{SellItem, CreateItem}
import inventory.domain.Item
import play.api.libs.json.Json

object JsonHelpers {

  implicit val itemFormatter = Json.format[Item]
  implicit val itemCreateFormatter = Json.format[CreateItem]
  implicit val sellItemFormatter = Json.format[SellItem]

}
