package inventory.storage

import inventory.events.{SellProduct, CreateProduct, Event}
import play.api.libs.json.{Json, JsResult, JsValue, Format}

object JsonFormatter {

  implicit val eventFormatter = new Format[Event] {
    def reads(js: JsValue): JsResult[Event] = {
      val eventType = (js \ "type").as[String]
      eventType match {
        case "CreateProduct" =>
          implicit val productCreateFormatter = Json.format[CreateProduct]

          val event = (js \ "event").get
          Json.fromJson[CreateProduct](event)
        case "SellProduct" =>
          implicit val sellProductFormatter = Json.format[SellProduct]

          val event = (js \ "event").get
          Json.fromJson[SellProduct](event)
      }
    }


    def writes(event: Event): JsValue = {
      event match {
        case c: CreateProduct =>
          implicit val productCreateFormatter = Json.format[CreateProduct]

          val json = Json.toJson(c)

          Json.obj("type" -> "CreateProduct", "event" -> json)
        case c: SellProduct =>
          implicit val sellProductFormatter = Json.format[SellProduct]

          val json = Json.toJson(c)

          Json.obj("type" -> "SellProduct", "event" -> json)
      }
    }
  }


}
