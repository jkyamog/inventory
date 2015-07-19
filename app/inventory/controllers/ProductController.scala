package inventory.controllers

import inventory.events.{UnappliedEvent, SellFailedNotification, SellProduct, CreateProduct}
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

    AggregateRoot.getById(id)(eventStore).map {
      case Success(product) => Ok(Json.toJson(product))
      case Failure(error) =>
        error match {
          case UnappliedEvent(event: SellProduct) =>
            // TODO: must only be done once
            eventStore saveEvent SellFailedNotification(event.productId, event.quantity)
        }

        Logger.error("failed to get productId: " + id, error)
        InternalServerError
    }
  }

  def sell(id: Long) = Action.async(parse.json) { request =>
    request.body.validate[SellProduct].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      sellProduct => {
        import Product.ProductAggregate

        val trySell = AggregateRoot.getById(id)(eventStore).map { productTry =>
          for {
            product <- productTry
            nextProduct <- ProductAggregate.apply(sellProduct)(product)
          } yield nextProduct
        }

        trySell.flatMap {
          case Success(_) =>
            for {
              (txId, eId) <- eventStore saveEvent (sellProduct, id)
            } yield Ok(Json.obj("txId" -> txId, "eId" -> eId))
          case Failure(error) =>
            Future.successful(InternalServerError)
        }
      }
    )
  }
}
