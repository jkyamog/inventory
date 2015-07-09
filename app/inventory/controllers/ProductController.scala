package inventory.controllers

import inventory.domain.Product.ProductAggregate
import inventory.events.CreateProduct
import inventory.storage.EventStore
import inventory.domain.{AggregateRoot, Product}

import play.api.libs.json._
import play.api.mvc._

class ProductController extends Controller {

  implicit val productFormatter = Json.format[Product]
  implicit val productCreateFormatter = Json.format[CreateProduct]

  def create = Action(parse.json) { request =>
    request.body.validate[CreateProduct].fold (
      errors => {
        BadRequest(JsError.toJson(errors))
      },
      createProduct => {
        val (txId, eId) = EventStore saveEvent createProduct
        Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }

  def get(id: Long) = Action { request =>
    import Product.ProductAggregate

    AggregateRoot.getById(id) match {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound
    }
  }
}
