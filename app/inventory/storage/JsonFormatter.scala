package inventory.storage

import inventory.events.{SellFailedNotification, ItemSold, ItemCreated, Event}
import play.api.libs.json.{Json, JsResult, JsValue, Format}

object JsonFormatter {

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = { // TODO refactor remove boiler plate
      val eventType = (js \ "type").as[String]
      eventType match {
        case "ItemCreated" =>
          implicit val productCreateFormatter = Json.format[ItemCreated]

          val event = (js \ "event").get
          Json.fromJson[ItemCreated](event)
        case "ItemSold" =>
          implicit val sellProductFormatter = Json.format[ItemSold]

          val event = (js \ "event").get
          Json.fromJson[ItemSold](event)
        case "SellFailedNotification" =>
          implicit val formatter = Json.format[SellFailedNotification]

          val event = (js \ "event").get
          Json.fromJson[SellFailedNotification](event)

      }
    }


    def writes(event: Event): JsValue = {
      event match {
        case c: ItemCreated =>
          implicit val productCreateFormatter = Json.format[ItemCreated]

          val json = Json.toJson(c)

          Json.obj("type" -> "ItemCreated", "event" -> json)
        case c: ItemSold =>
          implicit val sellProductFormatter = Json.format[ItemSold]

          val json = Json.toJson(c)

          Json.obj("type" -> "ItemSold", "event" -> json)
        case c: SellFailedNotification =>
          implicit val formatter = Json.format[SellFailedNotification]

          val json = Json.toJson(c)

          Json.obj("type" -> "SellFailedNotification", "event" -> json)

      }
    }
  }


}
