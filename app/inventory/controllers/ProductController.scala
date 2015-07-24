package inventory.controllers

import inventory.events.{FailedToApply, SellFailedNotification, SellProduct, CreateProduct}
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
  implicit val sellProductFormatter = Json.format[SellProduct]

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

    AggregateRoot.getById(id)(eventStore).map { product =>
      Ok(Json.toJson(product))
    }
  }

  def sell(id: Long) = Action.async(parse.json) { request =>
    request.body.validate[SellProduct].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      sellProduct => {
        import Product.ProductAggregate
          for {
            product <- AggregateRoot.getById(id)(eventStore)
            _ <- Future.fromTry(ProductAggregate.apply(sellProduct)(Some(product)))
            (txId, eId) <- eventStore saveEvent(sellProduct, id)
          } yield Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }
}
