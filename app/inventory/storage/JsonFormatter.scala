package inventory.storage

import inventory.events.{SellFailedNotification, ProductSold, ProductCreated, Event}
import play.api.libs.json.{Json, JsResult, JsValue, Format}

object JsonFormatter {

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = { // TODO refactor remove boiler plate
      val eventType = (js \ "type").as[String]
      eventType match {
        case "CreateProduct" =>
          implicit val productCreateFormatter = Json.format[ProductCreated]

          val event = (js \ "event").get
          Json.fromJson[ProductCreated](event)
        case "ProductSold" =>
          implicit val sellProductFormatter = Json.format[ProductSold]

          val event = (js \ "event").get
          Json.fromJson[ProductSold](event)
        case "SellFailedNotification" =>
          implicit val formatter = Json.format[SellFailedNotification]

          val event = (js \ "event").get
          Json.fromJson[SellFailedNotification](event)

      }
    }


    def writes(event: Event): JsValue = {
      event match {
        case c: ProductCreated =>
          implicit val productCreateFormatter = Json.format[ProductCreated]

          val json = Json.toJson(c)

          Json.obj("type" -> "CreateProduct", "event" -> json)
        case c: ProductSold =>
          implicit val sellProductFormatter = Json.format[ProductSold]

          val json = Json.toJson(c)

          Json.obj("type" -> "ProductSold", "event" -> json)
        case c: SellFailedNotification =>
          implicit val formatter = Json.format[SellFailedNotification]

          val json = Json.toJson(c)

          Json.obj("type" -> "SellFailedNotification", "event" -> json)

      }
    }
  }


}
