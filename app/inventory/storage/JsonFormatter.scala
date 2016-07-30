package inventory.storage

import inventory.events._
import play.api.libs.json.{Json, JsResult, JsValue, Format}

object JsonFormatter {

  import inventory.controllers.JsonHelpers._
  val ITEM_CREATED = ItemCreated.toString
  val ITEM_INCREASED = ItemIncreased.toString
  val ITEM_REDUCED = ItemReduced.toString
  val SELL_FAILED_NOTIFICATION = SellFailedNotification.toString
  val ITEM_ARCHIVED = ItemArchived.toString

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = {
      val eventType = (js \ "type").as[String]
      val event = (js \ "event").as[JsValue]

      eventType match {
        case ITEM_CREATED =>
          Json.fromJson[ItemCreated](event)

        case ITEM_INCREASED =>
          Json.fromJson[ItemIncreased](event)

        case ITEM_REDUCED =>
          Json.fromJson[ItemReduced](event)

        case SELL_FAILED_NOTIFICATION =>
          Json.fromJson[SellFailedNotification](event)
      }
    }


    def writes(event: Event): JsValue = event match {
      case j: ItemCreated =>
        Json.obj("type" -> ITEM_CREATED, "event" -> Json.toJson(j))

      case j: ItemIncreased =>
        Json.obj("type" -> ITEM_INCREASED, "event" -> Json.toJson(j))

      case j: ItemReduced =>
        Json.obj("type" -> ITEM_REDUCED, "event" -> Json.toJson(j))

      case j: SellFailedNotification =>
        Json.obj("type" -> SELL_FAILED_NOTIFICATION, "event" -> Json.toJson(j))

      case j: ItemArchived =>
        Json.obj("type" -> ITEM_ARCHIVED, "event" -> Json.toJson(j))
    }
  }


}
