package inventory.controllers

import inventory.events.CreateProduct
import inventory.storage.{SqlEventStore, EventStore}
import inventory.domain.{AggregateRoot, Product}
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Products extends ProductController {
  val eventStore = SqlEventStore
}

trait ProductController extends Controller {

  implicit val productFormatter = Json.format[Product]
  implicit val productCreateFormatter = Json.format[CreateProduct]

  val eventStore: EventStore

  def create = Action.async(parse.json) { request =>
    request.body.validate[CreateProduct].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      createProduct => {
        for {
          (txId, eId) <- eventStore saveEvent createProduct
        } yield Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }

  def get(id: Long) = Action.async { request =>
    import Product.ProductAggregate

    AggregateRoot.getById(id)(eventStore).map {
      case Success(product) => Ok(Json.toJson(product))
      case Failure(error) =>
        Logger.error("failed to get productId: " + id, error)
        InternalServerError
    }
  }
}
