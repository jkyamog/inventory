package inventory.storage

import inventory.events.{SellFailedNotification, ItemSold, ItemCreated, Event}
import play.api.libs.json.{Json, JsResult, JsValue, Format}

object JsonFormatter {

  import inventory.controllers.JsonHelpers._
  val ITEM_CREATED = ItemCreated.toString
  val ITEM_SOLD = ItemSold.toString
  val SELL_FAILED_NOTIFICATION = SellFailedNotification.toString

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = {
      val eventType = (js \ "type").as[String]
      eventType match {
        case ITEM_CREATED =>
          val event = (js \ "event").get
          Json.fromJson[ItemCreated](event)

        case ITEM_SOLD =>
          val event = (js \ "event").get
          Json.fromJson[ItemSold](event)

        case SELL_FAILED_NOTIFICATION =>
          val event = (js \ "event").get
          Json.fromJson[SellFailedNotification](event)

      }
    }


    def writes(event: Event): JsValue = {
      event match {
        case c: ItemCreated =>
          val json = Json.toJson(c)
          Json.obj("type" -> ITEM_CREATED, "event" -> json)

        case c: ItemSold =>
          val json = Json.toJson(c)
          Json.obj("type" -> ITEM_SOLD, "event" -> json)

        case c: SellFailedNotification =>
          val json = Json.toJson(c)
          Json.obj("type" -> SELL_FAILED_NOTIFICATION, "event" -> json)

      }
    }
  }


}
