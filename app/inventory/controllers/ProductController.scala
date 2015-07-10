package inventory.controllers

import inventory.events.CreateProduct
import inventory.storage.EventStore
import inventory.domain.{AggregateRoot, Product}

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future


class ProductController extends Controller {

  implicit val productFormatter = Json.format[Product]
  implicit val productCreateFormatter = Json.format[CreateProduct]

  def create = Action.async(parse.json) { request =>
    request.body.validate[CreateProduct].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      createProduct => {
        for {
          (txId, eId) <- EventStore saveEvent createProduct
        } yield Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }

  def get(id: Long) = Action.async { request =>
    import Product.ProductAggregate

    AggregateRoot.getById(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound
    }
  }
}
