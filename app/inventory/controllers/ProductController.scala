package inventory.controllers

import inventory.commands._
import inventory.events._
import inventory.storage.{SqlEventStore, EventStore}
import inventory.domain.{ProductCommand, ProductHelper, AggregateRoot, Product}
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Products extends ProductController {
  val eventStore = SqlEventStore
  inventory.reads.Products.init
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
        val productSold = ProductCommand(createProduct)(None)

        for {
          (txId, eId) <- eventStore saveEvent productSold
        } yield Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }

  def get(id: Long) = Action.async { request =>
    import ProductHelper.productEvents

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
        import ProductHelper.productEvents
          for {
            product <- AggregateRoot.getById(id)(eventStore)
            productSold = ProductCommand.apply(sellProduct)(Some(product))
            (txId, eId) <- eventStore saveEvent(productSold, id)
          } yield Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }
}
