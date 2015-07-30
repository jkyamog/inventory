package inventory.storage

import inventory.events.{ProductSold, ProductCreated, Event}
import play.api.libs.json.{Json, JsResult, JsValue, Format}

object JsonFormatter {

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = {
      val eventType = (js \ "type").as[String]
      eventType match {
        case "CreateProduct" =>
          implicit val productCreateFormatter = Json.format[ProductCreated]

          val event = (js \ "event").get
          Json.fromJson[ProductCreated](event)
        case "SellProduct" =>
          implicit val sellProductFormatter = Json.format[ProductSold]

          val event = (js \ "event").get
          Json.fromJson[ProductSold](event)
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

          Json.obj("type" -> "SellProduct", "event" -> json)
      }
    }
  }


}
